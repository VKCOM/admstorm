package com.vk.admstorm.configuration.kphp

import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.vk.admstorm.env.Env
import com.vk.admstorm.executors.BaseRunnableExecutor
import com.vk.admstorm.executors.KphpComplexRunExecutor
import com.vk.admstorm.executors.KphpRunExecutor
import com.vk.admstorm.git.sync.SyncChecker
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.utils.ServerNameProvider

class KphpConfigurationRunState(
    private val myEnv: ExecutionEnvironment,
    private val myRunConfiguration: KphpConfiguration
) : RunProfileState {
    private val myExecutor: BaseRunnableExecutor

    init {
        val command = buildCommand()

        myExecutor = if (needAdditionalHandling(myRunConfiguration.runType)) {
            KphpRunExecutor(myEnv.project, myRunConfiguration.runType, command)
        } else {
            KphpComplexRunExecutor(myEnv.project, myRunConfiguration.runType, command)
        }
    }

    private fun buildCommand(): String {
        val env = myRunConfiguration.envVariables
        val params = myRunConfiguration.parameters

        val command = when (myRunConfiguration.runType) {
            KphpRunType.Bu -> ""
            else -> myRunConfiguration.runType.command
        }

        val bin = when (myRunConfiguration.runType) {
            KphpRunType.My -> Env.data.vkCommand
            else -> Env.data.kphpCommand
        }

        return "$env $bin $command $params"
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
            when (myRunConfiguration.runType) {
                KphpRunType.Sc -> doKphpScript()
                else -> doKphp()
            }
        }
    }

    private fun doKphpScript() {
        invokeLater {
            KphpScriptRunner(myEnv.project, myRunConfiguration).run()
        }
    }

    private fun doKphp() {
        ApplicationManager.getApplication().executeOnPooledThread {
            myExecutor.run()
        }
    }

    private fun onCanceledSync() {
        AdmWarningNotification("Launch was canceled due to out of sync with the ${ServerNameProvider.name()}")
            .withTitle("KPHP run canceled")
            .withActions(
                AdmNotification.Action("Synchronize...") { _, notification ->
                    notification.expire()
                    doCheckSync()
                }
            )
            .show()
    }

    /**
     * Returns true if for this KPHP configuration [type] we will create
     * additional tabs in which we will put the processed output, false
     * means there will be only one tab with output in the executor.
     */
    private fun needAdditionalHandling(type: KphpRunType): Boolean {
        return when (type) {
            KphpRunType.Ge, KphpRunType.Co,
            KphpRunType.No, KphpRunType.My -> true
            else -> false
        }
    }
}
