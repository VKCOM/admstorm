package com.vk.admstorm.configuration.phpunit

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.vk.admstorm.env.Env
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.utils.MyPathUtils.remotePathByLocalPath
import com.vk.admstorm.utils.MySshUtils
import com.vk.admstorm.utils.PhpDebugUtils
import java.io.File

class RemotePhpUnitConfigurationRunState(
    private val env: ExecutionEnvironment,
    private val conf: RemotePhpUnitConfiguration
) : RunProfileState {

    companion object {
        fun executeRemotePhpUnitCommand(
            exec: Executor?,
            command: String,
            env: ExecutionEnvironment,
            runConfig: RemotePhpUnitConfiguration,
        ): DefaultExecutionResult? {
            if (!SshConnectionService.getInstance(env.project).isConnectedOrWarning()) {
                return null
            }

            if (exec == null) {
                return null
            }

            val workingDir =
                if (!runConfig.isApiTest) null
                else "${Env.data.projectRoot}/${Env.data.phpSourceFolder}"

            val consoleProperties = RemotePhpUnitConsoleProperties(runConfig, exec)
            val console = SMTestRunnerConnectionUtil
                .createConsole(
                    consoleProperties.testFrameworkName,
                    consoleProperties
                ) as SMTRunnerConsoleView

            val fullCommand = if (workingDir != null) "cd $workingDir && $command" else command
            val handler = MySshUtils.exec(env.project, fullCommand) ?: return null

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

    private fun getClassNameFromFQN(name: String): String {
        if (!name.contains("\\")) {
            return name
        }

        return name.substring(name.lastIndexOf('\\') + 1 until name.length)
    }

    private fun buildCommand(): String {
        val phpUnitXml = remotePathByLocalPath(env.project, conf.configPath)
        val phpUnitExe = remotePathByLocalPath(env.project, conf.phpUnitPath)

        val additional = conf.additionalParameters
        val base = "$phpUnitExe --teamcity --configuration $phpUnitXml $additional"

        if (conf.isDirectoryScope) {
            val remoteDir = remotePathByLocalPath(env.project, conf.directory)
            return "$base $remoteDir"
        }

        if (conf.isClassScope || conf.isMethodScope) {
            val className = getClassNameFromFQN(conf.className)
            val localDir = File(conf.filename).parentFile.path ?: ""
            val localFile = File(conf.filename).name
            val remoteDir = remotePathByLocalPath(env.project, localDir)

            val filter = if (conf.isMethodScope) {
                "$className::${conf.method}"
            } else className

            return "$base --filter $filter --test-suffix $localFile $remoteDir"
        }

        return base
    }

    override fun execute(exec: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        if (exec is DefaultDebugExecutor) {
            return runPhpUnitDebug(exec)
        }

        return runPhpUnit(exec)
    }

    private fun runPhpUnitDebug(exec: Executor?): ExecutionResult? {
        val project = env.project
        PhpDebugUtils.enablePhpDebug(project)
        val ok = PhpDebugUtils.checkSshTunnel(project) { runPhpUnitDebug(exec) }
        if (!ok) {
            return null
        }

        PhpDebugUtils.showNotificationAboutDelay(project)
        val command = buildCommand().removePrefix("vk ")
        val commandWithoutPhpUnit = command.substring(command.indexOf(' ') + 1)
        val fullCommand = "source ~/.php_debug && php_debug_test $commandWithoutPhpUnit"

        return executeRemotePhpUnitCommand(exec, fullCommand, env, conf)
    }

    private fun runPhpUnit(exec: Executor?): ExecutionResult? {
        return executeRemotePhpUnitCommand(exec, buildCommand(), env, conf)
    }
}
