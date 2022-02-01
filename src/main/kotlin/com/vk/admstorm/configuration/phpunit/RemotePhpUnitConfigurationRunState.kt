package com.vk.admstorm.configuration.phpunit

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
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MySshUtils
import java.io.File

class RemotePhpUnitConfigurationRunState(
    private val myEnv: ExecutionEnvironment,
    private val myRunConfiguration: RemotePhpUnitConfiguration
) : RunProfileState {

    companion object {
        fun executeRemotePhpUnitCommand(
            exec: Executor,
            command: String,
            env: ExecutionEnvironment,
            runConfig: RemotePhpUnitConfiguration
        ): DefaultExecutionResult? {
            if (!SshConnectionService.getInstance(env.project).isConnectedOrWarning()) {
                return null
            }

            val consoleProperties = RemotePhpUnitConsoleProperties(runConfig, exec)
            val console = SMTestRunnerConnectionUtil
                .createConsole(
                    consoleProperties.testFrameworkName,
                    consoleProperties
                ) as SMTRunnerConsoleView

            val handler = MySshUtils.exec(env.project, command) ?: return null

            console.attachToProcess(handler)

            val smTestProxy = console.resultsViewer.root as SMTestProxy.SMRootTestProxy
            smTestProxy.setTestsReporterAttached()
            smTestProxy.setSuiteStarted()

            val result = DefaultExecutionResult(console, handler)
            val rerunFailedTestsAction = RemotePhpUnitRerunFailedTestsAction(console, consoleProperties)
            rerunFailedTestsAction.setModelProvider { console.resultsViewer }
            result.setRestartActions(rerunFailedTestsAction)

            return result
        }
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

    private fun getClassNameFromFQN(name: String): String {
        if (!name.contains("\\")) {
            return name
        }

        return name.substring(name.lastIndexOf('\\') + 1 until name.length)
    }

    private fun buildCommand(): String {
        val phpUnitXml = "${Env.data.projectRoot}/phpunit.xml"
        val phpunit =
            if (myRunConfiguration.useParatest)
                Env.data.phpunitCommand.replace("phpunit", "paratest")
            else
                Env.data.phpunitCommand
        val additional = myRunConfiguration.additionalParameters
        val base = "$phpunit --teamcity --configuration $phpUnitXml $additional"

        if (myRunConfiguration.isDirectoryScope) {
            val remoteDir = MyPathUtils.remotePathByLocalPath(myEnv.project, myRunConfiguration.directory)
            return "$base $remoteDir"
        }

        if (myRunConfiguration.isClassScope || myRunConfiguration.isMethodScope) {
            val className = getClassNameFromFQN(myRunConfiguration.className)
            val localDir = File(myRunConfiguration.filename).parentFile.path ?: ""
            val localFile = File(myRunConfiguration.filename).name
            val remoteDir = MyPathUtils.remotePathByLocalPath(myEnv.project, localDir)

            val filter = if (myRunConfiguration.isMethodScope) {
                "$className::${myRunConfiguration.method}"
            } else className

            return "$base --filter $filter --test-suffix $localFile $remoteDir"
        }

        return base
    }

    override fun execute(exec: Executor, runner: ProgramRunner<*>): ExecutionResult? {
        ApplicationManager.getApplication().invokeAndWait {
            doCheckSync()
        }

        val command = buildCommand()
        return executeRemotePhpUnitCommand(exec, command, myEnv, myRunConfiguration)
    }
}
