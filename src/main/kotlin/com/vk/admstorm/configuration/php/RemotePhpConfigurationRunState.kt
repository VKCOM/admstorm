package com.vk.admstorm.configuration.php

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.jetbrains.php.debug.PhpProjectDebugConfiguration
import com.jetbrains.php.debug.listener.PhpDebugExternalConnectionsAccepter
import com.vk.admstorm.CommandRunner.runRemotely
import com.vk.admstorm.env.Env
import com.vk.admstorm.executors.RemotePhpRunExecutor
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MySshUtils
import com.vk.admstorm.utils.MyUtils.invokeAndWaitResult
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

        val accepter = PhpDebugExternalConnectionsAccepter.getInstance(project)
        if (!accepter.isStarted) {
            PhpDebugExternalConnectionsAccepter.getInstance(project).doSwitch()
        }

        if (!checkSshTunnels(project)) {
            return null
        }

        showNotificationAboutDelay()

        val command = buildDebugCommand(myRunConfiguration.scriptName)

        val output = runRemotely(project, command)
        if (output.exitCode != 0) {
            LOG.warn(output.stderr)
        }

        return DefaultExecutionResult()
    }

    private fun checkSshTunnels(project: Project): Boolean {
        val ports = PhpProjectDebugConfiguration.getInstance(project).state.xDebugDebugPorts
        val hasOpenTunnels = ports.any { port ->
            runRemotely(project, "nc -z localhost $port").exitCode == 0
        }

        if (hasOpenTunnels) {
            return true
        }

        AdmWarningNotification(
            """
                It seems that the SSH tunnel is not open for ports ${ports.joinToString(", ")},
                which are described in Xdebug ports.
                <br>
                <br>
                Open the tunnel manually and try again.
             """.trimIndent()
        )
            .withTitle("SSH tunnel is not open")
            .withActions(
                AdmNotification.Action("Open tunnel on ${ports.first()} port") { _, notification ->
                    notification.expire()

                    MySshUtils.openTunnel(project, ports.first(), object : ProcessListener {
                        private var myWasRestarted = false

                        override fun startNotified(event: ProcessEvent) {}

                        override fun processTerminated(event: ProcessEvent) {
                            if (event.exitCode != 0) {
                                AdmWarningNotification(
                                    """
                                        ${event.text}
                                        <br>
                                        <br>
                                        Try opening it manually.
                                        """.trimIndent()
                                )
                                    .withTitle("Unable to open SSH tunnel on port ${ports.first()}")
                                    .show()
                            }
                        }

                        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                            val mark = "rtype keepalive@openssh.com want_reply 1"
                            if (event.text.contains(mark) && !myWasRestarted) {
                                AdmNotification()
                                    .withTitle("SSH tunnel on port ${ports.first()} successfully opened")
                                    .show()

                                runPhpDebug()

                                myWasRestarted = true
                            }
                        }

                    })
                }
            )
            .show()

        return false
    }

    private fun showNotificationAboutDelay() {
        val key = "dont.need.show.notification.about.delay.for.php.remote.debug"
        val dontNeedShow = PropertiesComponent.getInstance(myEnv.project).getBoolean(key)

        if (dontNeedShow) {
            return
        }

        AdmNotification(
            "After starting debugging, there is a slight delay before debugging starts inside the IDE, be patient."
        )
            .withTitle("Remote PHP debug delay")
            .withActions(
                AdmNotification.Action("Don't show again") { e, notification ->
                    notification.expire()
                    PropertiesComponent.getInstance(myEnv.project).setValue(key, true)
                }
            )
            .show()
    }

    private fun runPhp(): ExecutionResult {
        val executor = invokeAndWaitResult {
            RemotePhpRunExecutor(myEnv.project, buildCommand(myRunConfiguration.scriptName))
        }

        executor.run()

        return DefaultExecutionResult()
    }
}
