package com.vk.admstorm.configuration.kbench

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project

open class KBenchConfigurationFactory(type: ConfigurationType) :
    ConfigurationFactory(type) {

    override fun getId() = KBenchConfigurationType.ID

    override fun createTemplateConfiguration(project: Project) =
        KBenchConfiguration(project, this, "KPHP Bench")

    override fun getOptionsClass() = KBenchConfigurationOptions::class.java
}
