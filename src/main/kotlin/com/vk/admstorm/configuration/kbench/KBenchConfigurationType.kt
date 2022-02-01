package com.vk.admstorm.configuration.kbench

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.vk.admstorm.ui.MyIcons

class KBenchConfigurationType : ConfigurationTypeBase(
    ID, "KPHP Bench",
    "KPHP Bench configuration type",
    MyIcons.kphpBench
) {
    companion object {
        const val ID = "KBenchConfiguration"
    }

    init {
        addFactory(KBenchConfigurationFactory(this))
    }
}
