package com.vk.admstorm.configuration.runanything

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.vk.admstorm.executors.RunAnythingExecutor
import com.vk.admstorm.utils.MyUtils.invokeAndWaitResult

class RunAnythingConfigurationRunState(
    private val myEnv: ExecutionEnvironment,
    private val myRunConfiguration: RunAnythingConfiguration
) : RunProfileState {

    private fun buildCommand(): String {
        val env = myRunConfiguration.envVariables
        val params = myRunConfiguration.parameters
        return "$env $params"
    }

    override fun execute(exec: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        val executor = invokeAndWaitResult {
            RunAnythingExecutor(myEnv.project, buildCommand())
        }

        executor.run()

        return DefaultExecutionResult()
    }
}
