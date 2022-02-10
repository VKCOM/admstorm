package com.vk.admstorm.configuration.builders

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project

open class BuildersConfigurationFactory(type: ConfigurationType) :
    ConfigurationFactory(type) {

    override fun getId() = BuildersConfigurationType.ID

    override fun createTemplateConfiguration(project: Project) =
        BuildersConfiguration(project, this, "Builders")

    override fun getOptionsClass() = BuildersConfigurationOptions::class.java
}
