package com.vk.admstorm.configuration.kphp

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project

open class KphpConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId() = KphpConfigurationType.ID

    override fun createTemplateConfiguration(project: Project) =
        KphpConfiguration(project, this, "KPHP")

    override fun getOptionsClass() = KphpConfigurationOptions::class.java
}
