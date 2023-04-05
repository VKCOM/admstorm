package com.vk.admstorm.services

import com.intellij.execution.RunManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.vk.admstorm.configuration.runanything.RunAnythingConfiguration
import com.vk.admstorm.configuration.runanything.RunAnythingConfigurationFactory
import com.vk.admstorm.configuration.runanything.RunAnythingConfigurationType
import com.vk.admstorm.executors.RunAnythingExecutor
import com.vk.admstorm.utils.MyUtils.executeOnPooledThread
import com.vk.admstorm.utils.ServerNameProvider

@Service(Service.Level.PROJECT)
class RunAnythingOnServerService(private val project: Project) {
    companion object {
        fun getInstance(project: Project) = project.service<RunAnythingOnServerService>()
    }

    fun run(command: String, saveAsConfiguration: Boolean) {
        if (saveAsConfiguration) {
            val configurationSettings = RunManager.getInstance(project)
                .createConfiguration(
                    "Run '$command' on ${ServerNameProvider.name()}",
                    RunAnythingConfigurationFactory(RunAnythingConfigurationType())
                )

            val configuration = configurationSettings.configuration as RunAnythingConfiguration
            configuration.parameters = command

            RunManager.getInstance(project).addConfiguration(configurationSettings)
        }

        invokeLater {
            val executor = RunAnythingExecutor(project, command)
            executeOnPooledThread {
                executor.run()
            }
        }
    }
}
