package com.vk.admstorm.services

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.settings.AdmStormSettingsState
import io.sentry.Attachment
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.OperatingSystem
import io.sentry.protocol.SentryId
import io.sentry.protocol.SentryRuntime
import io.sentry.protocol.User
import org.apache.commons.io.input.ReversedLinesFileReader
import java.io.File
import java.nio.charset.StandardCharsets

@Service
class SentryService(project: Project) {
    companion object {
        private val LOG = logger<SentryService>()

        const val PLUGIN_ID = "com.vk.admstorm"
        const val MAX_READ_LINES = 300

        fun getInstance(project: Project) = project.service<SentryService>()
    }

    init {
        val config = ConfigService.getInstance(project)
        val plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))
        val application = ApplicationInfo.getInstance()
        val user = project.let {
            GitUtils.localUser(it)
        }

        if (config.sentryDsn.isEmpty()) {
            LOG.info("Sending errors to Sentry is disabled")
        }

        Sentry.init { options ->
            options.dsn = config.sentryDsn
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

    fun sendError(t: Throwable?): SentryId = sendEvent(SentryLevel.ERROR, t)

    fun sendWarn(t: Throwable?): SentryId = sendEvent(SentryLevel.WARNING, t)

    private fun sendEvent(level: SentryLevel, t: Throwable?): SentryId {
        var sentryId = SentryId.EMPTY_ID

        Sentry.withScope { scope ->
            val file = readIDEALogFile()
            scope.addAttachment(Attachment(file, "idea.log"))

            val sentryEvents = SentryEvent().also {
                it.throwable = t
                it.level = level
            }

            sentryId = Sentry.captureEvent(sentryEvents)
        }

        return sentryId
    }

    private fun readIDEALogFile(): ByteArray {
        val logFile = File(PathManager.getLogPath(), "idea.log")
        val reader = ReversedLinesFileReader(logFile, StandardCharsets.UTF_8)

        var lines = ""
        for (i in 0 until MAX_READ_LINES) {
            val line: String = reader.readLine() ?: break
            lines += ("$line\n")
        }

        return lines.toByteArray(Charsets.UTF_8)
    }
}
