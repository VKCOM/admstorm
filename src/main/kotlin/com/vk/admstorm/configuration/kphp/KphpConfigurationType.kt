package com.vk.admstorm.configuration.kphp

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.vk.admstorm.ui.AdmIcons

class KphpConfigurationType : ConfigurationTypeBase(
    ID, "KPHP",
    "KPHP configuration type",
    AdmIcons.General.Kphp
) {
    companion object {
        const val ID = "KphpConfiguration"
    }

    init {
        addFactory(KphpConfigurationFactory(this))
    }
}
