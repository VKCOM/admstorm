package com.vk.admstorm

import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEventMulticasterEx
import com.intellij.openapi.fileTypes.ex.FileTypeChooser
import com.intellij.openapi.fileTypes.impl.AbstractFileType
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.IdeFrame
import com.intellij.serviceContainer.AlreadyDisposedException
import com.intellij.ssh.SshException
import com.intellij.util.messages.MessageBusConnection
import com.vk.admstorm.env.Env
import com.vk.admstorm.git.sync.SyncChecker
import com.vk.admstorm.highlight.CppTypeHighlightPatcher
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.settings.AdmStormSettingsState
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MyUtils.measureTime
import com.vk.admstorm.utils.MyUtils.runBackground
import com.vk.admstorm.utils.ServerNameProvider
import java.util.concurrent.atomic.AtomicBoolean

@Service
class AdmStormStartupActivity : StartupActivity {
    companion object {
        private val LOG = Logger.getInstance(AdmStormStartupActivity::class.java)

        private var myConnection: MessageBusConnection? = null

        fun getInstance(project: Project) = project.service<AdmStormStartupActivity>()
    }

    private var myFocusListenerIsSet = false
    private val myCheckSyncRunning = AtomicBoolean(false)

    private fun checkSyncSilently(project: Project) {
        myCheckSyncRunning.set(true)

        runBackground(project, "AdmStorm: Check sync with ${ServerNameProvider.name()}") {
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
                myCheckSyncRunning.set(false)
            }
        }
    }

    fun afterConnectionTasks(project: Project, onReady: Runnable? = null) {
        ProgressManager.getInstance().run(object : BackgroundableTask(
            project,
            "AdmStorm: Resolve env data",
        ) {
            override fun run() {
                myIndicator.isIndeterminate = false
                myIndicator.fraction = 0.05

                try {
                    step(0.7) {
                        measureTime(LOG, "resolve env data") {
                            Env.resolve(project)
                        }
                    }

                    step("AdmStorm: Resolve root", 0.8) {
                        measureTime(LOG, "resolve root") {
                            MyPathUtils.resolveRemoteRoot(project)
                        }
                    }

                    step(1.0) {
                        checkSyncSilently(project)
                        setListenerForEditorsFocus(project)
                    }

                    onReady?.run()
                } catch (e: SshException) {
                    LOG.warn("Unexpected exception while afterConnectionTasks", e)
                }
            }
        })
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
    private fun setListenerForEditorsFocus(project: Project) {
        if (myConnection != null) {
            LOG.info("IDE focus listener is already installed for '${project.name}'")
            return
        }

        if (EditorFactory.getInstance().eventMulticaster !is EditorEventMulticasterEx) {
            LOG.warn("'EditorFactory.getInstance().eventMulticaster' is not EditorEventMulticasterEx")
            return
        }

        myConnection = ApplicationManager.getApplication().messageBus.connect()
        myConnection?.subscribe(ApplicationActivationListener.TOPIC, object : ApplicationActivationListener {
            override fun applicationActivated(ideFrame: IdeFrame) {
                LOG.info("Application activated")

                if (!AdmStormSettingsState.getInstance().checkSyncOnFocus) {
                    LOG.info("Check has not started because it was disabled")
                    return
                }

                val connectionService = try {
                    SshConnectionService.getInstance(project)
                } catch (e: AlreadyDisposedException) {
                    LOG.info("Check has not started because project already disposed, disconnect")
                    myConnection?.disconnect()
                    myConnection = null
                    return
                }

                if (!connectionService.isConnected()) {
                    LOG.info("Check has not started because there is no SSH connection")
                    return
                }

                if (!myCheckSyncRunning.get()) {
                    LOG.info("Start check sync silently")
                    checkSyncSilently(project)
                } else {
                    LOG.info("Check has not started because the previous check has not finished yet")
                }
            }

            override fun applicationDeactivated(ideFrame: IdeFrame) = LOG.info("Application deactivated")
        })

        myFocusListenerIsSet = true
    }

    override fun runActivity(project: Project) {
        if (!AdmService.getInstance(project).needBeEnabled()) {
            // We don't connect if this is not a vkcom project
            return
        }

        measureTime(LOG, "patch cpp highlight") {
            val cppType = FileTypeChooser.getKnownFileTypeOrAssociate(".c") as AbstractFileType
            CppTypeHighlightPatcher.patch(cppType)
        }

        if (AdmStormSettingsState.getInstance().connectWhenProjectStarts) {
            SshConnectionService.getInstance(project).tryConnectSilence {
                afterConnectionTasks(project)
            }
        }
    }
}
