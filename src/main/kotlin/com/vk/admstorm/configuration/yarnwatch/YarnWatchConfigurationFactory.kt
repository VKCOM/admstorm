package com.vk.admstorm.configuration.yarnwatch

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project

open class YarnWatchConfigurationFactory(type: ConfigurationType) :
    ConfigurationFactory(type) {

    override fun getId() = YarnWatchConfigurationType.ID

    override fun createTemplateConfiguration(project: Project) =
        YarnWatchConfiguration(project, this, "YarnWatch")

    override fun getOptionsClass() = YarnWatchConfigurationOptions::class.java
}
