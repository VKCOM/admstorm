package com.vk.admstorm.configuration.runanything

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.icons.AllIcons

class RunAnythingConfigurationType : ConfigurationTypeBase(
    ID, "Run anything on server",
    "Run anything on server configuration type",
    AllIcons.RunConfigurations.Compound
) {
    companion object {
        const val ID = "RunAnythingConfiguration"
    }

    init {
        addFactory(RunAnythingConfigurationFactory(this))
    }
}
