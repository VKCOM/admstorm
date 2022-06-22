package com.vk.admstorm.diagnostic

import com.intellij.openapi.diagnostic.DefaultLogger
import com.vk.admstorm.services.SentryService

class AdmStormLogger(private val sentry: SentryService, delegateFactory: Factory, category: String) : DefaultLogger(category) {
    private val delegate = delegateFactory.getLoggerInstance(category)

    override fun debug(message: String) {
        delegate.debug(message)
    }

    override fun debug(t: Throwable?) {
        delegate.debug(t)
    }

    override fun debug(message: String, t: Throwable?) {
        delegate.debug(message, t)
    }

    override fun info(message: String) {
        delegate.info(message)
    }

    override fun info(message: String, t: Throwable?) {
        delegate.info(message, t)
    }

    override fun warn(message: String, t: Throwable?) {
        delegate.warn(message, t)
        sentry.sendWarn(message, t)
    }

    override fun error(message: String, t: Throwable?, vararg details: String?) {
        delegate.error(message, t, *details)
        sentry.sendError(message, t)
    }
}
