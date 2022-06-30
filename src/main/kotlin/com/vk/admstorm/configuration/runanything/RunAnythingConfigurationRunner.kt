package com.vk.admstorm.configuration.runanything

import com.vk.admstorm.configuration.WithSshConfigurationRunner

class RunAnythingConfigurationRunner : WithSshConfigurationRunner(
    withDebug = false,
    withCoverage = false,
    inEDT = false,
    RunAnythingConfiguration::class
) {
    override fun getRunnerId() = "RunAnything"
}
