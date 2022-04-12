package com.vk.admstorm.configuration.php

import com.vk.admstorm.configuration.WithSshConfigurationRunner

class RemotePhpRunner : WithSshConfigurationRunner(withDebug = true, inEDT = false, RemotePhpConfiguration::class) {
    override fun getRunnerId() = "RemotePhp"
}
