package com.vk.admstorm.configuration.builders

import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.ApplicationManager
import com.vk.admstorm.env.Env
import com.vk.admstorm.executors.BuildersExecutor
import com.vk.admstorm.git.sync.SyncChecker
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.ssh.SshConnectionService

class BuildersConfigurationRunState(
    private val myEnv: ExecutionEnvironment,
    private val myRunConfiguration: BuildersConfiguration
) : RunProfileState {

    private val myExecutor = BuildersExecutor(myEnv.project, buildCommand())

    private fun buildCommand(): String {
        return "${Env.data.vkCommand} builders ${myRunConfiguration.parameters}"
    }

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        if (!SshConnectionService.getInstance(myEnv.project).isConnectedOrWarning()) {
            return null
        }

        doCheckSync()
        return null
    }

    private fun doCheckSync() {
        SyncChecker.getInstance(myEnv.project).doCheckSyncSilentlyTask({
            onCanceledSync()
        }) {
            ApplicationManager.getApplication().executeOnPooledThread {
                myExecutor.run()
            }
        }
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
