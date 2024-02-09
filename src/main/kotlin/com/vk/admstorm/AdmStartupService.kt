package com.vk.admstorm

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.util.RunOnceUtil
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEventMulticasterEx
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.IssueNavigationConfiguration
import com.intellij.openapi.vcs.IssueNavigationLink
import com.intellij.openapi.wm.IdeFrame
import com.intellij.serviceContainer.AlreadyDisposedException
import com.intellij.ssh.SshException
import com.intellij.util.messages.MessageBusConnection
import com.vk.admstorm.env.Env
import com.vk.admstorm.env.getByKey
import com.vk.admstorm.git.sync.SyncChecker
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.settings.AdmStormSettingsState
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MyUtils
import com.vk.admstorm.utils.ServerNameProvider
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class AdmStartupService(private var project: Project) {
    companion object {
        private val LOG = logger<AdmStartupService>()

        fun getInstance(project: Project) = project.service<AdmStartupService>()
    }

    private var focusListenerIsSet = false
    private val checkSyncRunning = AtomicBoolean(false)
    private var connection: MessageBusConnection? = null

    private fun checkSyncSilently() {
        checkSyncRunning.set(true)

        MyUtils.runBackground(project, "AdmStorm: Check sync with ${ServerNameProvider.name()}") {
            try {
                val state = SyncChecker.getInstance(project).currentState()
                if (state == SyncChecker.State.Unknown) {
                    LOG.warn("No SSH connection")
                    return@runBackground
                }

                if (state == SyncChecker.State.Ok) {
                    return@runBackground
                }

                val subject = when (state) {
                    SyncChecker.State.BranchNotSync -> "branch"
                    SyncChecker.State.LastCommitNotSync -> "last commit"
                    SyncChecker.State.FilesNotSync -> "files"
                    else -> "repository"
                }

                AdmWarningNotification("", true)
                    .withTitle("Local $subject not sync with ${ServerNameProvider.name()}")
                    .withActions(
                        AdmNotification.Action("Show details") { _, notification ->
                            notification.expire()
                            SyncChecker.getInstance(project).doCheckSyncSilentlyTask(null) {
                                AdmNotification()
                                    .withTitle("Local and ${ServerNameProvider.name()} have been synced successfully")
                                    .show()
                            }
                        }
                    )
                    .show()
            } finally {
                checkSyncRunning.set(false)
            }
        }
    }

    fun afterConnectionTasks(onReady: Runnable? = null) {
        ProgressManager.getInstance().run(object : BackgroundableTask(
            project,
            "AdmStorm: Resolve env data",
        ) {
            override fun run() {
                myIndicator.isIndeterminate = false
                myIndicator.fraction = 0.05

                try {
                    step(0.7) {
                        MyUtils.measureTime(LOG, "resolve env data") {
                            Env.resolve(project)
                        }
                    }

                    step("AdmStorm: Resolve root", 0.8) {
                        MyUtils.measureTime(LOG, "resolve root") {
                            MyPathUtils.resolveRemoteRoot(project)
                        }
                    }

                    step(0.9) {
                        checkSyncSilently()
                        setListenerForEditorsFocus()
                    }

                    step(1.0) {
                        setSentryUser()
                        setupIssueTrackers(project)
                    }

                    showWelcomeMessage()

                    onReady?.run()

                    LOG.warn("Project '${project.name}' is ready")
                } catch (e: SshException) {
                    LOG.warn("Unexpected exception while afterConnectionTasks", e)
                }
            }
        })
    }

    private fun setupIssueTrackers(project: Project) {
        val navigationConfig = IssueNavigationConfiguration.getInstance(project)
        val service = Env.data.services.getByKey("jira") ?: return
        val url = service.url + "/browse/\$0"

        val isExist = navigationConfig.links.any { it.linkRegexp == url }
        if (!isExist) {
            val jiraLink = IssueNavigationLink("[A-Z]{2}[A-Z_0-9]*\\-\\d+", url)
            navigationConfig.links.add(jiraLink)
        }
    }

    private fun showWelcomeMessage() {
        val plugin = PluginManagerCore.getPlugin(AdmService.PLUGIN_ID) ?: return
        RunOnceUtil.runOnceForApp("com.vk.admstorm.welcome.test.message.${plugin.version}") {
            AdmNotification(
                """
                    The plugin has been successfully updated to version ${plugin.version}.
                    See <a href="https://vkcom.github.io/admstorm/whatsnew.html?server_name=${ServerNameProvider.name()}">what's new</a> in the plugin.
                """.trimIndent()
            ).show(project)
        }
    }

    /**
     * Sets up a listener that every time the IDE has focus, it starts
     * sync checking in the background.
     *
     * This can be useful if some changes were made on the server through
     * the console, and then the user switched to the IDE, thanks to this
     * listener, the user will be immediately shown a notification with a
     * quick action for synchronization.
     *
     * This behavior can be disabled in the settings.
     *
     * Note:
     * Since the listener is per-application but project-specific, we needed
     * to disconnect the connection every time the project is closed to create
     * a new connection with the new project, otherwise we would get
     * AlreadyDisposedException.
     */
    private fun setListenerForEditorsFocus() {
        if (connection != null) {
            LOG.info("IDE focus listener is already installed for '${project.name}'")
            return
        }

        if (EditorFactory.getInstance().eventMulticaster !is EditorEventMulticasterEx) {
            LOG.warn("'EditorFactory.getInstance().eventMulticaster' is not EditorEventMulticasterEx")
            return
        }

        connection = ApplicationManager.getApplication().messageBus.connect()
        connection?.subscribe(ApplicationActivationListener.TOPIC, object :
            ApplicationActivationListener {
            override fun applicationActivated(ideFrame: IdeFrame) {
                LOG.info("Application activated")

                if (!AdmStormSettingsState.getInstance().checkSyncOnFocus) {
                    LOG.info("Check has not started because it was disabled")
                    return
                }

                val connectionService = try {
                    SshConnectionService.getInstance(project)
                } catch (e: AlreadyDisposedException) {
                    LOG.info("Check has not started because project already disposed, disconnect", e)
                    connection?.disconnect()
                    connection = null
                    return
                }

                if (!connectionService.isConnected()) {
                    LOG.info("Check has not started because there is no SSH connection")
                    return
                }

                if (!checkSyncRunning.get()) {
                    LOG.info("Start check sync silently")
                    checkSyncSilently()
                } else {
                    LOG.info("Check has not started because the previous check has not finished yet")
                }
            }

            override fun applicationDeactivated(ideFrame: IdeFrame) = LOG.info("Application deactivated")
        })

        focusListenerIsSet = true
    }

    private fun setSentryUser() {
        val config = AdmStormSettingsState.getInstance()
        if (config.userNameForSentry.isNotEmpty()) {
            return
        }

        val output = CommandRunner.runRemotely(project, "whoami")
        if (output.exitCode != 0 || output.stdout == null) {
            LOG.warn("Error while getting username from server")
            return
        }

        config.userNameForSentry = output.stdout.trim()
    }
}
