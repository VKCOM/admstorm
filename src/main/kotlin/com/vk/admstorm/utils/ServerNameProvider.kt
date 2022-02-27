package com.vk.admstorm.utils

import com.vk.admstorm.env.Env

object ServerNameProvider {
    fun name() = Env.data.serverName.ifEmpty { "dev-server" }
    fun uppercase() = name().replaceFirstChar { it.uppercaseChar() }
}
