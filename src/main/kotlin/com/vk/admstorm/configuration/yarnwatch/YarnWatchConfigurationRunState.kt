package com.vk.admstorm.configuration.yarnwatch

import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.project.Project
import com.vk.admstorm.YarnWatchService

class YarnWatchConfigurationRunState(private val project: Project) : RunProfileState {
    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        val service = YarnWatchService.getInstance(project)
        if (service.isRunning()) {
            service.stop()
        }
        service.start()
        return null
    }
}
