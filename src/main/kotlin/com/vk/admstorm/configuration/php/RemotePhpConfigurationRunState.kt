package com.vk.admstorm.configuration.php

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.diagnostic.logger
import com.vk.admstorm.CommandRunner.runRemotely
import com.vk.admstorm.env.Env
import com.vk.admstorm.executors.RemotePhpRunExecutor
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MyUtils.invokeAndWaitResult
import com.vk.admstorm.utils.PhpDebugUtils
import java.io.File

class RemotePhpConfigurationRunState(
    private val myEnv: ExecutionEnvironment,
    private val myRunConfiguration: RemotePhpConfiguration
) : RunProfileState {

    companion object {
        private val LOG = logger<RemotePhpConfigurationRunState>()
    }

    private fun buildCommand(scriptName: String): String {
        if (File(scriptName).isAbsolute) {
            val path = MyPathUtils.remotePathByLocalPath(myEnv.project, scriptName)
            return "php $path"
        }

        return "php ${Env.data.projectRoot}/${Env.data.phpSourceFolder}/$scriptName"
    }

    private fun buildDebugCommand(scriptName: String): String {
        val path = if (File(scriptName).isAbsolute) {
            MyPathUtils.remotePhpFolderRelativePathByLocalPath(myEnv.project, scriptName)
        } else {
            scriptName
        }

        return "source ~/.php_debug && cd ${MyPathUtils.resolveRemoteRoot(myEnv.project)}/www && php_debug $path"
    }

    override fun execute(exec: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        if (exec is DefaultDebugExecutor) {
            return runPhpDebug()
        }

        return runPhp()
    }

    private fun runPhpDebug(): DefaultExecutionResult? {
        val project = myEnv.project

        PhpDebugUtils.enablePhpDebug(project)
        val ok = PhpDebugUtils.checkSshTunnel(project) { runPhpDebug() }
        if (!ok) {
            return null
        }

        PhpDebugUtils.showNotificationAboutDelay(project)

        val command = buildDebugCommand(myRunConfiguration.scriptName)

        val output = runRemotely(project, command)
        if (output.exitCode != 0) {
            LOG.warn(output.stderr)
        }

        return DefaultExecutionResult()
    }

    private fun runPhp(): ExecutionResult {
        val executor = invokeAndWaitResult {
            RemotePhpRunExecutor(myEnv.project, buildCommand(myRunConfiguration.scriptName))
        }

        executor.run()

        return DefaultExecutionResult()
    }
}
