package com.vk.admstorm.diagnostic

import com.intellij.openapi.diagnostic.Logger

class AnyStormLoggerFactory(private val factoryDelegate: Logger.Factory) : Logger.Factory {
    override fun getLoggerInstance(category: String): Logger {
        if (category.contains("com.vk.admstorm")) {
            return AnyStormLogger(factoryDelegate, category)
        }

        return factoryDelegate.getLoggerInstance(category)
    }
}
