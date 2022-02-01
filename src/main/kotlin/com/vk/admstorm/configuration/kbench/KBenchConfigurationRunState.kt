package com.vk.admstorm.configuration.kbench

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.openapi.application.ApplicationManager
import com.vk.admstorm.env.Env
import com.vk.admstorm.git.sync.SyncChecker
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.utils.MyKphpUtils
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MySshUtils

class KBenchConfigurationRunState(
    private val myEnv: ExecutionEnvironment,
    private val myRunConfiguration: KBenchConfiguration
) : RunProfileState {

    private fun buildCommand(scriptName: String): String {
        val scriptPath = MyPathUtils.remotePathByLocalPath(myEnv.project, scriptName)

        val includeDirsFlag =
            if (myRunConfiguration.benchType.command == "bench-php") ""
            else "--include-dirs='${MyKphpUtils.includeDirsAsList(myEnv.project).joinToString(",")}'"

        return "${Env.data.ktestCommand} ${myRunConfiguration.benchType.command}" +
                " --count ${myRunConfiguration.countRuns}" +
                " $includeDirsFlag" +
                " --teamcity" +
                " --disable-kphp-autoload" +
                " $scriptPath"
    }

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        ApplicationManager.getApplication().invokeAndWait {
            doCheckSync()
        }

        return doBench()
    }

    private fun doBench(): DefaultExecutionResult? {
        if (!SshConnectionService.getInstance(myEnv.project).isConnectedOrWarning()) {
            return null
        }

        val consoleProperties = KBenchConsoleProperties(myRunConfiguration, myEnv.executor)
        val console = SMTestRunnerConnectionUtil
            .createConsole(
                consoleProperties.testFrameworkName,
                consoleProperties
            ) as SMTRunnerConsoleView

        val command = buildCommand(myRunConfiguration.scriptName)
        val handler = MySshUtils.exec(myEnv.project, command) ?: return null

        console.attachToProcess(handler)

        val smTestProxy = console.resultsViewer.root as SMTestProxy.SMRootTestProxy
        smTestProxy.setTestsReporterAttached()
        smTestProxy.setSuiteStarted()

        return DefaultExecutionResult(console, handler)
    }

    private fun doCheckSync() {
        SyncChecker.getInstance(myEnv.project).doCheckSyncSilentlyTask({
            onCanceledSync()
        }) {}
    }

    private fun onCanceledSync() {
        AdmWarningNotification("Current launch may not be correct due to out of sync")
            .withTitle("Launch on out of sync")
            .withActions(
                AdmNotification.Action("Synchronize...") { _, notification ->
                    notification.expire()
                    SyncChecker.getInstance(myEnv.project).doCheckSyncSilentlyTask({}, {})
                }
            )
            .show()
    }
}
