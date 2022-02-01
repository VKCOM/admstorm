package com.vk.admstorm.configuration.kphp

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.vk.admstorm.ui.MyIcons

class KphpConfigurationType : ConfigurationTypeBase(
    ID, "KPHP",
    "KPHP configuration type",
    MyIcons.kphp
) {
    companion object {
        const val ID = "KphpConfiguration"
    }

    init {
        addFactory(KphpConfigurationFactory(this))
    }
}
