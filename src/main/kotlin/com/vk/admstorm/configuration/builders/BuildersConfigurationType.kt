package com.vk.admstorm.configuration.builders

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.icons.AllIcons

class BuildersConfigurationType : ConfigurationTypeBase(
    ID, "Builders",
    "Builders configuration type",
    AllIcons.Toolwindows.ToolWindowBuild
) {
    companion object {
        const val ID = "BuildersConfiguration"
    }

    init {
        addFactory(BuildersConfigurationFactory(this))
    }
}
