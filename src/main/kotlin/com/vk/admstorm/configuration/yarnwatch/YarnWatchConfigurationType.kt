package com.vk.admstorm.configuration.yarnwatch

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.vk.admstorm.ui.MyIcons

class YarnWatchConfigurationType : ConfigurationTypeBase(
    ID, "YarnWatch",
    "Yarn Watch configuration type",
    MyIcons.yarn
) {
    companion object {
        const val ID = "YarnWatchConfiguration"
    }

    init {
        addFactory(YarnWatchConfigurationFactory(this))
    }
}
