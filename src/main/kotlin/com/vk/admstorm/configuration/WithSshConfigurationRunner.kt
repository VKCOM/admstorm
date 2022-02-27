package com.vk.admstorm.configuration

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.ApplicationManager
import com.vk.admstorm.git.sync.SyncChecker
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.utils.ServerNameProvider

abstract class WithSshConfigurationRunner(
    private val withDebug: Boolean = false,
) : ProgramRunner<RunnerSettings?> {

    override fun canRun(s: String, runProfile: RunProfile): Boolean {
        return when {
            runProfile !is WithSshConfiguration -> false
            s == "Run" -> true
            withDebug && s == "Debug" -> true
            else -> false
        }
    }

    override fun execute(environment: ExecutionEnvironment) {
        if (!SshConnectionService.getInstance(environment.project).isConnectedOrWarning()) {
            return
        }

        runWithSyncCheck(environment)
    }

    private fun runWithSyncCheck(environment: ExecutionEnvironment) {
        SyncChecker.getInstance(environment.project).doCheckSyncSilentlyTask({
            onCanceledSync(environment)
        }) {
            ApplicationManager.getApplication().invokeAndWait {
                val state = environment.state

                ApplicationManager.getApplication().executeOnPooledThread {
                    state?.execute(environment.executor, this)
                }
            }
        }
    }

    private fun onCanceledSync(environment: ExecutionEnvironment) {
        AdmWarningNotification("Launch was canceled due to out of sync with the ${ServerNameProvider.name()}")
            .withTitle("Tool run canceled")
            .withActions(
                AdmNotification.Action("Synchronize...") { _, notification ->
                    notification.expire()
                    runWithSyncCheck(environment)
                }
            )
            .show()
    }
}
