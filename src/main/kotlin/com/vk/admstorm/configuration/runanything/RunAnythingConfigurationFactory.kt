package com.vk.admstorm.configuration.runanything

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project

open class RunAnythingConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId() = RunAnythingConfigurationType.ID

    override fun createTemplateConfiguration(project: Project) =
        RunAnythingConfiguration(project, this, "Run anything")

    override fun getOptionsClass() = RunAnythingConfigurationOptions::class.java
}
