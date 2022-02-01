package com.vk.admstorm.configuration.phplinter

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project

open class PhpLinterConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId() = PhpLinterConfigurationType.ID

    override fun createTemplateConfiguration(project: Project) =
        PhpLinterConfiguration(project, this, "PHP Linter")

    override fun getOptionsClass() = PhpLinterConfigurationOptions::class.java
}
