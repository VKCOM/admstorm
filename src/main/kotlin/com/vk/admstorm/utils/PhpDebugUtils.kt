package com.vk.admstorm.utils

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.jetbrains.php.debug.PhpProjectDebugConfiguration
import com.jetbrains.php.debug.listener.PhpDebugExternalConnectionsAccepter
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification

object PhpDebugUtils {
    fun enablePhpDebug(project: Project) {
        val accepter = PhpDebugExternalConnectionsAccepter.getInstance(project)
        if (!accepter.isStarted) {
            PhpDebugExternalConnectionsAccepter.getInstance(project).doSwitch()
        }
    }

    fun checkSshTunnel(project: Project, onStart: Runnable): Boolean {
        val ports = PhpProjectDebugConfiguration.getInstance(project).state.xDebugDebugPorts
        val hasOpenTunnels = ports.any { port ->
            CommandRunner.runRemotely(project, "nc -z localhost $port").exitCode == 0
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
                Plugin may try to automatically open the tunnel, or you may open the tunnel manually and try again.
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

                                onStart.run()

                                myWasRestarted = true
                            }
                        }
                    })
                }
            )
            .show()

        return false
    }

    fun showNotificationAboutDelay(project: Project) {
        val key = "dont.need.show.notification.about.delay.for.php.remote.debug"
        val dontNeedShow = PropertiesComponent.getInstance(project).getBoolean(key)

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
                    PropertiesComponent.getInstance(project).setValue(key, true)
                }
            )
            .show()
    }
}
