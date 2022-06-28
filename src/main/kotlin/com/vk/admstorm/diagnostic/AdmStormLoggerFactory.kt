package com.vk.admstorm.diagnostic

import com.intellij.openapi.diagnostic.Logger
import com.vk.admstorm.AdmService
import com.vk.admstorm.services.SentryService

class AdmStormLoggerFactory(private val sentry: SentryService, private val factoryDelegate: Logger.Factory) : Logger.Factory {
    override fun getLoggerInstance(category: String): Logger {
        if (category.contains(AdmService.PLUGIN_ID.idString)) {
            return AdmStormLogger(sentry, factoryDelegate, category)
        }

        return factoryDelegate.getLoggerInstance(category)
    }
}
