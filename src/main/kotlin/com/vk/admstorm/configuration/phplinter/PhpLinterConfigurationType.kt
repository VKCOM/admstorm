package com.vk.admstorm.configuration.phplinter

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.vk.admstorm.ui.AdmIcons

class PhpLinterConfigurationType : ConfigurationTypeBase(
    ID, "PHP Linter",
    "PHP Linter configuration type",
    AdmIcons.General.KhpLinter
) {
    companion object {
        const val ID = "PhpLinterConfiguration"
    }

    init {
        addFactory(PhpLinterConfigurationFactory(this))
    }
}
