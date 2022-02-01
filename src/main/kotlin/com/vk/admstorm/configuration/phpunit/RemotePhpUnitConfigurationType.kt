package com.vk.admstorm.configuration.phpunit

import com.intellij.execution.configurations.ConfigurationTypeBase
import icons.PhpIcons

class RemotePhpUnitConfigurationType : ConfigurationTypeBase(
    ID, "Remote PHPUnit",
    "Remote PHPUnit configuration type",
    PhpIcons.Phpunit
) {
    companion object {
        const val ID = "RemotePhpUnitConfiguration"
    }

    init {
        addFactory(RemotePhpUnitConfigurationFactory(this))
    }
}
