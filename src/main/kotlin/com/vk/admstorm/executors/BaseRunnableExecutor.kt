package com.vk.admstorm.executors

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.Output
import com.intellij.execution.OutputListener
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.*
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.remote.ColoredRemoteProcessHandler
import com.intellij.ssh.process.SshExecProcess
import com.vk.admstorm.actions.ActionToolbarFastEnableAction
import com.vk.admstorm.console.Console
import com.vk.admstorm.executors.tabs.Tab
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.utils.MySshUtils
import com.vk.admstorm.utils.MyUtils.copyToClipboard
import com.vk.admstorm.utils.MyUtils.createHaste
import com.vk.admstorm.utils.MyUtils.invokeAfter
import com.vk.admstorm.utils.ServerNameProvider
import javax.swing.Icon
import javax.swing.JComponent

abstract class BaseRunnableExecutor(protected val config: Config, protected val project: Project) : Disposable {
    companion object {
        private val LOG = logger<BaseRunnableExecutor>()
    }

    data class Config(
        val tabName: String = "Tool Tab",
        val layoutName: String = tabName,
        val command: String = "",
        val workingDir: String? = null
    )

    private val actionGroup = DefaultActionGroup()
    private val tabs = mutableListOf<Tab>()
    private val console = Console(project)

    private lateinit var layout: RunnerLayoutUi
    private lateinit var processHandler: ColoredRemoteProcessHandler<SshExecProcess>
    private lateinit var outputListener: OutputListener

    private val restartAction = object : ActionToolbarFastEnableAction(
        "Rerun ${config.tabName}", AllIcons.Actions.Restart,
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            if (!processHandler.isProcessTerminated) {
                onStopBeforeRerun()
                stop()
                invokeAfter(1_000) {
                    run()
                    onRerun()
                    stopAction.setEnabled(true)
                }
                return
            }

            run()
            onRerun()

            stopAction.setEnabled(true)
        }
    }

    private val stopAction = object : ActionToolbarFastEnableAction(
        "Stop ${config.tabName}", AllIcons.Actions.Suspend
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            stop()
            onStop()
        }
    }

    private val clearOutputsAction = object : ActionToolbarFastEnableAction(
        "Clear outputs", AllIcons.Actions.GC
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            console.clear()
            clearTabs()
        }
    }

    private val copyOutputAction = object : ActionToolbarFastEnableAction(
        "Copy launch output", AllIcons.Actions.Copy
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            val output = outputListener.output.stdout + outputListener.output.stderr
            copyToClipboard(output)
        }
    }

    private val hasteOutputAction = object : ActionToolbarFastEnableAction(
        "Haste launch output", AllIcons.Actions.MoveTo2
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            ApplicationManager.getApplication().executeOnPooledThread {
                val output = outputListener.output.stdout + outputListener.output.stderr
                val link = createHaste(e.project!!, output)
                copyToClipboard(link)

                AdmNotification()
                    .withTitle("Link to hastebin copied to clipboard")
                    .show()
            }
        }
    }

    init {
        actionGroup.addAll(actions())

        project.messageBus.connect().subscribe(
            ToolWindowManagerListener.TOPIC,
            ToolWindowListener()
        )
    }

    /**
     * Starts the execution of a command in a new console.
     *
     * Shows the new Tool Window on the new tab.
     *
     * Since the plugin sends an SSH command to start execution, it's
     * advisable to execute this method in a separate thread
     * so that there is no UI freeze.
     */
    fun run() = runImpl()

    /**
     * Stops the executed by [run] process.
     */
    fun stop() {
        stopAction.setEnabled(false)

        try {
            processHandler.destroyProcess()
        } catch (e: Exception) {
            LOG.warn("Failed to stop process", e)
        }
    }

    /**
     * Adds a new tab to the tool window before creation.
     */
    fun withTab(tab: Tab) {
        tabs.add(tab)
    }

    /**
     * Adds a new tab to the tool window after creation.
     */
    fun addTab(tab: Tab) {
        tab.addAsContentTo(layout)
    }

    /**
     * Selects and focus passed tab.
     */
    fun selectTab(tab: Tab) {
        layout.selectAndFocus(tab.content, true, true)
    }

    /**
     * Show the tool window.
     */
    fun showToolWindow() = invokeLater {
        toolWindow()?.activate(null, true, true)
    }

    /**
     * @return true, if the tool window is visible, false otherwise
     */
    fun isToolWindowVisible() = toolWindow()?.isVisible == true

    /**
     * @return the [Executor] instance for this executor.
     */
    open fun executorInstance() = AdmToolsExecutor.getRunExecutorInstance()

    /**
     * @return tool window ID for this executor.
     */
    open fun toolWindowId() = AdmToolsExecutor.TOOL_WINDOW_ID

    /**
     * @return runner title for this executor.
     */
    open fun runnerTitle() = ServerNameProvider.uppercase()

    /**
     * @return runner ID for this executor.
     */
    open fun runnerId() = "Adm"

    /**
     * Hook to run when the User clicks the "Stop" button.
     */
    open fun onStop() {}

    /**
     * Hook to run when the User clicks the "Rerun" button.
     */
    open fun onStopBeforeRerun() {}

    /**
     * Hook to run when the User clicks the "Rerun" button.
     *
     * Runs after [onStopBeforeRerun].
     */
    open fun onRerun() {}

    /**
     * Hook to run when a process is finished.
     */
    open fun onFinish() {}

    /**
     * Hook to run when a tool window is shown.
     */
    open fun onToolWindowShow() {}

    /**
     * @return icon for this executor.
     */
    abstract fun icon(): Icon

    /**
     * @return additional listeners for this executor.
     */
    open fun listeners() = emptyList<ProcessAdapter>()

    /**
     * @return process output
     */
    fun output(): Output = outputListener.output

    override fun dispose() {
        console.dispose()
    }

    private fun actions(): List<AnAction> = listOf(
        restartAction,
        stopAction,
        Separator.create(),
        copyOutputAction,
        hasteOutputAction,
        Separator.create(),
        clearOutputsAction
    )

    private fun runImpl() {
        clearTabs()

        processHandler = MySshUtils.exec(project, config.command, config.command, config.workingDir) ?: return
        console.view().attachToProcess(processHandler)
        console.clear()

        setupListeners()

        ApplicationManager.getApplication().invokeAndWait {
            prepareAndShowInterface()
        }

        processHandler.startNotify()
    }

    private fun setupListeners() {
        outputListener = object : OutputListener() {
            override fun processTerminated(event: ProcessEvent) {
                super.processTerminated(event)

                console.println(
                    "\nProcess finished with exit code ${event.exitCode}",
                    if (event.exitCode == 0) ConsoleViewContentType.USER_INPUT
                    else ConsoleViewContentType.ERROR_OUTPUT
                )

                onFinish()

                invokeLater {
                    showToolWindow()

                    if (event.exitCode != 1) {
                        stopAction.setEnabled(false)
                    }
                }
            }
        }

        processHandler.addProcessListener(outputListener)

        listeners().forEach {
            processHandler.addProcessListener(it)
        }
    }

    private fun prepareAndShowInterface() {
        stopAction.setEnabled(true)

        layout = RunnerLayoutUi.Factory.getInstance(project)
            .create(runnerId(), runnerTitle(), config.layoutName, this)

        val executor = executorInstance()

        val runProfile = object : RunProfile {
            override fun getState(e: Executor, ee: ExecutionEnvironment) = null
            override fun getName(): String = config.tabName
            override fun getIcon(): Icon = icon()
        }

        val descriptor =
            RunContentDescriptor(runProfile, DefaultExecutionResult(console.view(), processHandler), layout)

        descriptor.executionId = System.nanoTime()
        descriptor.setFocusComputable { console.view().preferredFocusableComponent }
        descriptor.isAutoFocusContent = true
        descriptor.contentToolWindowId = toolWindowId()

        val rawOutputComponentWithActions =
            SimpleComponentWithActions(console.view() as JComponent?, console.component())

        val rawOutputTab = layout.createContent(
            ExecutionConsole.CONSOLE_CONTENT_ID,
            rawOutputComponentWithActions,
            "Output",
            AllIcons.Debugger.Console,
            console.component()
        )
        rawOutputTab.description = "Launch output"
        rawOutputTab.isCloseable = false

        Disposer.register(project, descriptor)
        Disposer.register(descriptor, this)
        Disposer.register(descriptor, rawOutputTab)

        layout.addContent(rawOutputTab, 0, PlaceInGrid.right, false)
        layout.options.setLeftToolbar(actionGroup, "RunnerToolbar")

        // Add additional tabs.
        tabs.forEach { tab ->
            tab.addAsContentTo(layout)
        }

        RunContentManager.getInstance(project).showRunContent(executor!!, descriptor)

        showToolWindow()
    }

    private fun clearTabs() {
        tabs.forEach { it.clear() }
    }

    private fun toolWindow() = ToolWindowManager.getInstance(project).getToolWindow(toolWindowId())

    private inner class ToolWindowListener : ToolWindowManagerListener {
        override fun toolWindowShown(toolWindow: ToolWindow) {
            if (toolWindow.id == toolWindowId()) {
                onToolWindowShow()
            }
        }
    }
}
