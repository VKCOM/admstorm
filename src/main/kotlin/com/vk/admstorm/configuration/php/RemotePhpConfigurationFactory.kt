package com.vk.admstorm.configuration.php

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project

open class RemotePhpConfigurationFactory(type: ConfigurationType) :
    ConfigurationFactory(type) {

    override fun getId() = RemotePhpConfigurationType.ID

    override fun createTemplateConfiguration(project: Project) =
        RemotePhpConfiguration(project, this, "Remote PHP")

    override fun getOptionsClass() = RemotePhpConfigurationOptions::class.java
}
