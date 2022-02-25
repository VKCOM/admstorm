package com.vk.admstorm.configuration.php

import com.intellij.execution.configurations.ConfigurationTypeBase
import icons.PhpIcons

class RemotePhpConfigurationType : ConfigurationTypeBase(
    ID, "Remote PHP",
    "Remote PHP configuration type",
    PhpIcons.PhpRemote
) {
    companion object {
        const val ID = "RemotePhpConfiguration"
    }

    init {
        addFactory(RemotePhpConfigurationFactory(this))
    }
}
