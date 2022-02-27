package com.vk.admstorm.configuration.runanything

import com.vk.admstorm.configuration.WithSshConfigurationRunner

class RunAnythingConfigurationRunner : WithSshConfigurationRunner(withDebug = false) {
    override fun getRunnerId() = "RunAnything"
}
