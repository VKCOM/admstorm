package com.vk.admstorm.configuration.yarnwatch

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.vk.admstorm.ui.AdmIcons

class YarnWatchConfigurationType : ConfigurationTypeBase(
    ID, "YarnWatch",
    "Yarn Watch configuration type",
    AdmIcons.General.Yarn
) {
    companion object {
        const val ID = "YarnWatchConfiguration"
    }

    init {
        addFactory(YarnWatchConfigurationFactory(this))
    }
}
