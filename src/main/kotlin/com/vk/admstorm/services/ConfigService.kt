package com.vk.admstorm.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Service
class ConfigService {
    companion object {
        fun getInstance() = service<ConfigService>()
    }

    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    @Serializable
    data class PluginConfig(@SerialName("sentry_dsn") val sentryDsn: String = "")

    private val config: PluginConfig

    init {
        val configFile = javaClass.classLoader.getResourceAsStream("plugin_config.json")
            ?: throw IllegalStateException("Config file 'plugin_config.json' not found!")

        val configText = configFile.bufferedReader().use {
            it.readText()
        }

        config = Json.decodeFromString(configText)
    }

    val sentryDsn: String = config.sentryDsn
}
