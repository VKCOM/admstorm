package com.vk.admstorm.executors

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.OutputListener
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
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
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.remote.ColoredRemoteProcessHandler
import com.intellij.ssh.process.SshExecProcess
import com.intellij.util.ui.JBUI
import com.vk.admstorm.actions.ActionToolbarFastEnableAction
import com.vk.admstorm.env.Env
import com.vk.admstorm.executors.tabs.ConsoleTab
import com.vk.admstorm.executors.tabs.ProblemsTab
import com.vk.admstorm.executors.tabs.Tab
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.utils.MySshUtils
import com.vk.admstorm.utils.MyUtils
import javax.swing.Icon
import javax.swing.JComponent

abstract class BaseRunnableExecutor(protected var myConfig: Config, protected var myProject: Project) :
    ActionToolbarPanel(myProject, myConfig.name) {

    data class Config(
        var name: String = "Tool Tab",
        var command: String = "",
        var workingDir: String = Env.data.projectRoot
    )

    protected lateinit var myLayout: RunnerLayoutUi
    protected lateinit var myProcessHandler: ColoredRemoteProcessHandler<SshExecProcess>
    protected lateinit var myOutputListener: OutputListener

    private val myTabs = mutableListOf<Tab>()

    private val myRestartAction = object : ActionToolbarFastEnableAction(
        myConfig.name + " Start", "",
        AllIcons.Actions.Restart,
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            if (!myProcessHandler.isProcessTerminated) {
                myProcessHandler.destroyProcess()
                Thread.sleep(1_000)
            }

            run()
        }

        init {
            registerCustomShortcutSet(CommonShortcuts.getRerun(), myToolbarComponent)
        }
    }

    private val myStopAction = object : ActionToolbarFastEnableAction(
        myConfig.name + " Stop", "",
        AllIcons.Actions.Suspend
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            myProcessHandler.destroyProcess()
            setEnabled(false)
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
        myStopAction.setEnabled(false)
        myActionGroup.addAll(actions())
    }

    fun withTab(tab: Tab): BaseRunnableExecutor {
        myTabs.add(tab)
        return this
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

                ApplicationManager.getApplication().invokeLater {
                    activateToolWindow()
                    myStopAction.setEnabled(false)
                }
            }
        }

        myProcessHandler.addProcessListener(myOutputListener)

        ApplicationManager.getApplication().invokeAndWait {
            prepareAndShowInterface()
        }

        myProcessHandler.startNotify()
    }

    private fun prepareAndShowInterface() {
        myStopAction.setEnabled(true)

        myLayout = RunnerLayoutUi.Factory.getInstance(myProject)
            .create("Adm", "Adm Tool", myConfig.name, this)

        val executor = AdmToolsExecutor.getRunExecutorInstance()

        val runProfile = object : RunProfile {
            override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment):
                    RunProfileState? = null

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
        descriptor.contentToolWindowId = AdmToolsExecutor.TOOL_WINDOW_ID

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

        activateToolWindow()
    }

    private fun clearTabs() {
        myTabs.forEach { tab ->
            when (tab) {
                is ConsoleTab -> tab.console.clear()
                is ProblemsTab -> tab.panel = JBUI.Panels.simplePanel()
            }
        }
    }

    private fun activateToolWindow() {
        ApplicationManager.getApplication().invokeLater {
            ToolWindowManager.getInstance(myProject).getToolWindow(AdmToolsExecutor.TOOL_WINDOW_ID)!!
                .activate(null, true, true)
        }
    }

    abstract fun onReady()
    abstract fun icon(): Icon

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
