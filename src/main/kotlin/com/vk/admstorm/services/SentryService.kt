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
import com.vk.admstorm.AdmService
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.settings.AdmStormSettingsState
import io.sentry.Attachment
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.*
import org.apache.commons.io.input.ReversedLinesFileReader
import java.io.File
import java.nio.charset.StandardCharsets

@Service
class SentryService(project: Project) {
    companion object {
        private val LOG = logger<SentryService>()
        private const val MAX_FULL_LOG_READ_LINES = 2000
        private const val MAX_LOGGING_READ_LINES = 500

        fun getInstance(project: Project) = project.service<SentryService>()
    }

    private val user: GitUtils.User?

    init {
        val config = ConfigService.getInstance(project)
        val plugin = PluginManagerCore.getPlugin(PluginId.getId(AdmService.PLUGIN_ID))
        val application = ApplicationInfo.getInstance()

        user = project.let {
            GitUtils.localUser(it)
        }

        if (config.sentryDsn.isEmpty()) {
            LOG.info("Sending errors to Sentry is disabled")
        }

        Sentry.init { options ->
            options.dsn = config.sentryDsn
            options.release = plugin?.version ?: "UNKNOWN"
            options.isEnableNdk = false
            options.isDebug = true

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

    fun sendError(message: String, t: Throwable?): SentryId = sendEvent(SentryLevel.ERROR, message, t)

    fun sendWarn(message: String, t: Throwable?): SentryId = sendEvent(SentryLevel.WARNING, message, t)

    fun sendIdeaLog(): SentryId = sendEvent(SentryLevel.INFO, "Logs by ${user?.name}", withFullLog = true)

    private fun sendEvent(level: SentryLevel, message: String, t: Throwable? = null, withFullLog: Boolean = false): SentryId {
        var sentryId = SentryId.EMPTY_ID

        Sentry.withScope { scope ->
            val file = readIdeaLogFile(withFullLog)
            scope.addAttachment(Attachment(file, "idea.log"))

            val sentryEvents = SentryEvent().also { event ->
                event.throwable = t
                event.level = level

                event.message = Message().also { msg ->
                    msg.message = message
                }
            }

            sentryId = Sentry.captureEvent(sentryEvents)
            LOG.info("Sentry event sent: $sentryId")
        }

        return sentryId
    }

    private fun readIdeaLogFile(full: Boolean = false): ByteArray {
        val logFile = File(PathManager.getLogPath(), "idea.log")
        val countNeedLines = if (full) {
            MAX_FULL_LOG_READ_LINES
        } else {
            MAX_LOGGING_READ_LINES
        }

        val reader = ReversedLinesFileReader(logFile, StandardCharsets.UTF_8)

        var lines = ""
        for (i in 0 until countNeedLines) {
            val line = reader.readLine() ?: break
            lines += "$line\n"
        }

        return lines.toByteArray(StandardCharsets.UTF_8)
    }
}
