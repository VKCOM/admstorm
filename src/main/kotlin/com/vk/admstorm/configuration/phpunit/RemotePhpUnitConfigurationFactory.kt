package com.vk.admstorm.configuration.phpunit

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project

open class RemotePhpUnitConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId() = RemotePhpUnitConfigurationType.ID

    override fun createTemplateConfiguration(project: Project) =
        RemotePhpUnitConfiguration(project, this, "Remote PHPUnit")

    override fun getOptionsClass() = RemotePhpUnitConfigurationOptions::class.java
}
