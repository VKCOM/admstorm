package com.vk.admstorm.services

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.settings.AdmStormSettingsState
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.OperatingSystem
import io.sentry.protocol.SentryId
import io.sentry.protocol.SentryRuntime
import io.sentry.protocol.User

@Service
class SentryService(project: Project) {
    companion object {
        const val PLUGIN_ID = "com.vk.admstorm"

        fun getInstance(project: Project) = project.service<SentryService>()
    }

    init {
        val plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))
        val application = ApplicationInfo.getInstance()
        val user = project.let {
            GitUtils.localUser(it)
        }

        Sentry.init { options ->
            options.dsn = "sensitive information"
            options.release = plugin?.version ?: "UNKNOWN"

            options.setBeforeSend { event, _ ->
                val os = OperatingSystem().apply {
                    name = SystemInfo.OS_NAME
                    version = "${SystemInfo.OS_VERSION}-${SystemInfo.OS_ARCH}"
                }
                event.contexts.setOperatingSystem(os)

                val runtime = SentryRuntime().apply {
                    name = application.versionName
                    version = application.fullVersion
                }
                event.contexts.setRuntime(runtime)

                event.user = User().apply {
                    id = AdmStormSettingsState.getInstance().userNameForSentry
                    email = user.email
                    username = user.name
                }

                event.serverName = null

                event
            }
        }
    }

    fun sendError(t: Throwable?): SentryId {
        val sentryEvents = SentryEvent().apply {
            throwable = t
            level = SentryLevel.ERROR
        }

        return Sentry.captureEvent(sentryEvents)
    }
}
