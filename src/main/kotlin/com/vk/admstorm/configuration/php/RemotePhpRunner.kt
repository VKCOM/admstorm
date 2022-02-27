package com.vk.admstorm.configuration.php

import com.vk.admstorm.configuration.WithSshConfigurationRunner

class RemotePhpRunner : WithSshConfigurationRunner(withDebug = true) {
    override fun getRunnerId() = "RemotePhp"
}
