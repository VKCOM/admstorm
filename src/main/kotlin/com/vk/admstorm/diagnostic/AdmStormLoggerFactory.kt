package com.vk.admstorm.diagnostic

import com.intellij.openapi.diagnostic.Logger

class AdmStormLoggerFactory(private val factoryDelegate: Logger.Factory) : Logger.Factory {
    override fun getLoggerInstance(category: String): Logger {
        if (category.contains("com.vk.admstorm")) {
            return AdmStormLogger(factoryDelegate, category)
        }

        return factoryDelegate.getLoggerInstance(category)
    }
}
