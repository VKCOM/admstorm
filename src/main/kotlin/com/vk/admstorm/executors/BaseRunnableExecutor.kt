package com.vk.admstorm.executors

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.OutputListener
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.*
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
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
import com.intellij.util.ui.JBUI
import com.vk.admstorm.actions.ActionToolbarFastEnableAction
import com.vk.admstorm.executors.tabs.ConsoleTab
import com.vk.admstorm.executors.tabs.ProblemsTab
import com.vk.admstorm.executors.tabs.Tab
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.utils.MySshUtils
import com.vk.admstorm.utils.MyUtils
import com.vk.admstorm.utils.ServerNameProvider
import javax.swing.Icon
import javax.swing.JComponent

abstract class BaseRunnableExecutor(
    protected var myConfig: Config,
    protected var myProject: Project,
    private val needActivateToolWindow: Boolean = true,
) : ActionToolbarPanel(myProject, myConfig.name) {

    companion object {
        private val LOG = logger<BaseRunnableExecutor>()
    }

    data class Config(
        var name: String = "Tool Tab",
        var layoutName: String = name,
        var command: String = "",
        var workingDir: String? = null
    )

    protected lateinit var myLayout: RunnerLayoutUi
    protected lateinit var myProcessHandler: ColoredRemoteProcessHandler<SshExecProcess>
    protected lateinit var myOutputListener: OutputListener

    private val myTabs = mutableListOf<Tab>()

    private val myRestartAction = object : ActionToolbarFastEnableAction(
        "Rerun " + myConfig.name, "",
        AllIcons.Actions.Restart,
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            if (!myProcessHandler.isProcessTerminated) {
                onStopBeforeRerun()
                stop()
                Thread.sleep(1_000)
            }

            run()
            onRerun()

            myStopAction.setEnabled(true)
        }

        init {
            registerCustomShortcutSet(CommonShortcuts.getRerun(), myToolbarComponent)
        }
    }

    private val myStopAction = object : ActionToolbarFastEnableAction(
        "Stop " + myConfig.name, "",
        AllIcons.Actions.Suspend
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            stop()
            onStop()
        }
    }

    private val myClearOutputsAction = object : ActionToolbarFastEnableAction(
        "Clear outputs", "",
        AllIcons.Actions.GC
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            myConsole.clear()
            clearTabs()
        }
    }

    private val myCopyOutputAction = object : ActionToolbarFastEnableAction(
        "Copy launch output", "",
        AllIcons.Actions.Copy
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            val output = myOutputListener.output.stdout + myOutputListener.output.stderr
            MyUtils.copyToClipboard(output)
        }
    }

    private val myHasteOutputAction = object : ActionToolbarFastEnableAction(
        "Haste launch output", "",
        AllIcons.Actions.MoveTo2
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            ApplicationManager.getApplication().executeOnPooledThread {
                val output = myOutputListener.output.stdout + myOutputListener.output.stderr
                val link = MyUtils.createHaste(e.project!!, output)
                MyUtils.copyToClipboard(link)
                AdmNotification()
                    .withTitle("Link to hastebin copied to clipboard")
                    .show()
            }
        }
    }

    init {
        myActionGroup.addAll(actions())

        project.messageBus.connect().subscribe(
            ToolWindowManagerListener.TOPIC,
            ToolWindowListener()
        )
    }

    fun withTab(tab: Tab): BaseRunnableExecutor {
        myTabs.add(tab)
        return this
    }

    fun stop() {
        myStopAction.setEnabled(false)

        try {
            myProcessHandler.destroyProcess()
        } catch (e: Exception) {
            LOG.warn("Failed to stop process", e)
        }
    }

    /**
     * Starts the execution of a command in a new console.
     *
     * Shows the new Tool Window on the AdmTool tab.
     *
     * Since an SSH command is sent to start execution, it is
     * advisable to execute this method in a separate thread
     * so that there is no UI freeze.
     */
    fun run() {
        clearTabs()

        myProcessHandler = MySshUtils.exec(project, myConfig.command, myConfig.command, myConfig.workingDir) ?: return
        myConsole.view().attachToProcess(myProcessHandler)
        myConsole.clear()

        myOutputListener = object : OutputListener() {
            override fun processTerminated(event: ProcessEvent) {
                super.processTerminated(event)

                myConsole.println(
                    "\nProcess finished with exit code ${event.exitCode}",
                    if (event.exitCode == 0) ConsoleViewContentType.USER_INPUT
                    else ConsoleViewContentType.ERROR_OUTPUT
                )

                onReady()

                invokeLater {
                    showToolWindow()
                    if (event.exitCode != 1) {
                        myStopAction.setEnabled(false)
                    }
                }
            }
        }

        myProcessHandler.addProcessListener(myOutputListener)
        listeners().forEach {
            myProcessHandler.addProcessListener(it)
        }

        ApplicationManager.getApplication().invokeAndWait {
            prepareAndShowInterface()
        }

        myProcessHandler.startNotify()
    }

    private fun prepareAndShowInterface() {
        myStopAction.setEnabled(true)

        myLayout = RunnerLayoutUi.Factory.getInstance(myProject)
            .create(runnerId(), runnerTitle(), myConfig.layoutName, this)

        val executor = executorInstance()

        val runProfile = object : RunProfile {
            override fun getState(e: Executor, ee: ExecutionEnvironment) = null
            override fun getName(): String = myConfig.name
            override fun getIcon(): Icon = icon()
        }

        val consoleComponent = myConsole.component()
        val descriptor = RunContentDescriptor(
            runProfile, DefaultExecutionResult(myConsole.view(), myProcessHandler), myLayout
        )

        descriptor.executionId = System.nanoTime()
        descriptor.setFocusComputable { myConsole.view().preferredFocusableComponent }
        descriptor.isAutoFocusContent = true
        descriptor.contentToolWindowId = executorToolWindowId()

        val rawOutputComponentWithActions =
            SimpleComponentWithActions(myConsole.view() as JComponent?, consoleComponent)

        val rawOutputTab = myLayout.createContent(
            ExecutionConsole.CONSOLE_CONTENT_ID,
            rawOutputComponentWithActions,
            "Output",
            AllIcons.Debugger.Console,
            consoleComponent
        )
        rawOutputTab.description = "Launch output"
        rawOutputTab.isCloseable = false

        Disposer.register(myProject, descriptor)
        Disposer.register(descriptor, this)
        Disposer.register(descriptor, rawOutputTab)

        myLayout.addContent(rawOutputTab, 0, PlaceInGrid.right, false)
        myLayout.options.setLeftToolbar(myActionGroup, "RunnerToolbar")

        myTabs.forEach { tab ->
            tab.addAsContentTo(myLayout)
        }

        RunContentManager.getInstance(myProject).showRunContent(executor!!, descriptor)

        if (needActivateToolWindow) {
            showToolWindow()
        }
    }

    private fun clearTabs() {
        myTabs.forEach { tab ->
            when (tab) {
                is ConsoleTab -> tab.console.clear()
                is ProblemsTab -> tab.panel = JBUI.Panels.simplePanel()
            }
        }
    }

    fun showToolWindow() {
        invokeLater {
            ToolWindowManager.getInstance(myProject).getToolWindow(executorToolWindowId())
                ?.activate(null, true, true)
        }
    }

    fun isToolWindowVisible() =
        ToolWindowManager.getInstance(myProject).getToolWindow(executorToolWindowId())?.isVisible == true

    inner class ToolWindowListener : ToolWindowManagerListener {
        override fun toolWindowShown(toolWindow: ToolWindow) {
            if (toolWindow.id == executorToolWindowId()) {
                onToolWindowShow()
            }
        }
    }

    open fun executorInstance() = AdmToolsExecutor.getRunExecutorInstance()
    open fun executorToolWindowId() = AdmToolsExecutor.TOOL_WINDOW_ID
    open fun runnerTitle() = ServerNameProvider.uppercase()
    open fun runnerId() = "Adm"

    open fun onStop() {}
    open fun onStopBeforeRerun() {}
    open fun onRerun() {}
    open fun onReady() {}
    open fun onToolWindowShow() {}

    abstract fun icon(): Icon

    open fun listeners() = emptyList<ProcessAdapter>()

    fun output() = myOutputListener.output

    fun actions(): Collection<AnAction> = listOf(
        myRestartAction,
        myStopAction,
        Separator.create(),
        myCopyOutputAction,
        myHasteOutputAction,
        Separator.create(),
        myClearOutputsAction
    )
}
