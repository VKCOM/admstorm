package com.vk.admstorm.services

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.vk.admstorm.AdmService
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.settings.AdmStormSettingsState
import com.vk.admstorm.utils.MyUtils.readIdeaLogFile
import io.sentry.Attachment
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.*
import java.nio.charset.StandardCharsets

@Service(Service.Level.PROJECT)
class SentryService(project: Project) {
    companion object {
        private val LOG = logger<SentryService>()

        fun getInstance(project: Project) = project.service<SentryService>()
    }

    private val user = GitUtils.currentUser(project)

    init {
        initSentry()
    }

    private fun initSentry() {
        if (ApplicationManager.getApplication().isInternal) {
            LOG.info("Sending errors to Sentry is disabled. The IDE is running in development mode")
            return
        }

        val config = ConfigService.getInstance()
        if (config.sentryDsn.isEmpty()) {
            LOG.info("Sending errors to Sentry is disabled. DSN is not specified")
            return
        }

        val plugin = PluginManagerCore.getPlugin(AdmService.PLUGIN_ID)
        val application = ApplicationInfo.getInstance()

        LOG.info("Sending errors to Sentry is enabled")
        Sentry.init { options ->
            options.dsn = config.sentryDsn
            options.release = plugin?.version ?: "UNKNOWN"
            options.isEnableNdk = false

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

                val systemUserName = AdmStormSettingsState.getInstance().userNameForSentry
                event.user = User().apply {
                    id = systemUserName
                    email = user?.email
                    username = user?.name ?: systemUserName
                }

                event.serverName = null

                event
            }
        }
    }

    fun sendError(message: String?, t: Throwable?): SentryId = sendEvent(SentryLevel.ERROR, message, t)

    fun sendWarn(message: String, t: Throwable?): SentryId = sendEvent(SentryLevel.WARNING, message, t)

    private fun sendEvent(level: SentryLevel, message: String?, t: Throwable? = null, withFullLog: Boolean = false): SentryId {
        var sentryId = SentryId.EMPTY_ID

        Sentry.withScope { scope ->
            val fileContent = readIdeaLogFile(withFullLog).toByteArray(StandardCharsets.UTF_8)
            scope.addAttachment(Attachment(fileContent, "idea.log"))

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
}
