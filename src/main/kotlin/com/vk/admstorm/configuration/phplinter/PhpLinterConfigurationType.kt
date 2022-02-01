package com.vk.admstorm.configuration.phplinter

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.vk.admstorm.ui.MyIcons

class PhpLinterConfigurationType : ConfigurationTypeBase(
    ID, "PHP Linter",
    "PHP Linter configuration type",
    MyIcons.phpLinter
) {
    companion object {
        const val ID = "PhpLinterConfiguration"
    }

    init {
        addFactory(PhpLinterConfigurationFactory(this))
    }
}
