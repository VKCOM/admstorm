package com.vk.admstorm.configuration.phplinter

import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.ApplicationManager
import com.vk.admstorm.env.Env
import com.vk.admstorm.executors.PhpLinterExecutor
import com.vk.admstorm.git.sync.SyncChecker
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.utils.ServerNameProvider

class PhpLinterConfigurationRunState(
    private val myEnv: ExecutionEnvironment,
    private val myRunConfiguration: PhpLinterConfiguration
) : RunProfileState {

    private val myExecutor = PhpLinterExecutor(myEnv.project, buildCommand())

    private fun buildCommand(): String {
        val env = if (myRunConfiguration.runAsInTeamcity) {
            "PHP_LINTER_FULL=1 "
        } else ""

        return "$env${Env.data.phpLinterCommand} ${myRunConfiguration.parameters}"
    }

    override fun execute(exec: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
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
            doLint()
        }
    }

    private fun doLint() {
        ApplicationManager.getApplication().executeOnPooledThread {
            myExecutor.run()
        }
    }

    private fun onCanceledSync() {
        AdmWarningNotification("Launch was canceled due to out of sync with the ${ServerNameProvider.name()}")
            .withTitle("PHP Linter run canceled")
            .withActions(
                AdmNotification.Action("Synchronize...") { _, notification ->
                    notification.expire()
                    doCheckSync()
                }
            )
            .show()
    }
}
