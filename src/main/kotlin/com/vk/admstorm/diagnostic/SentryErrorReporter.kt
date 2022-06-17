package com.vk.admstorm.diagnostic

import com.intellij.diagnostic.IdeaReportingEvent
import com.intellij.ide.DataManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.diagnostic.SubmittedReportInfo.SubmissionStatus
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Consumer
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.utils.MyUtils
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.OperatingSystem
import io.sentry.protocol.SentryId
import io.sentry.protocol.SentryRuntime
import io.sentry.protocol.User
import java.awt.Component

class SentryErrorReporter : ErrorReportSubmitter() {
    init {
        val plugin = PluginManagerCore.getPlugin(PluginId.getId("com.vk.admstorm"))
        val application = ApplicationInfo.getInstance()

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

                event.serverName = null

                event
            }
        }
    }

    override fun getReportActionText(): String {
        return "Report to Sentry"
    }

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>
    ): Boolean {
        val project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(parentComponent))
        val user = project?.let {
            GitUtils.localUser(it)
        }

        for (event in events) {
            if (event !is IdeaReportingEvent) {
                continue
            }

            val sentryEvents = SentryEvent().apply {
                throwable = event.data.throwable
                level = SentryLevel.ERROR
            }

            sentryEvents.user = User().apply {
                id = null  // TODO
                email = user?.email
                username = user?.name
            }

            val sentryId = Sentry.captureEvent(sentryEvents)
            if (sentryId != SentryId.EMPTY_ID) {
                consumer.consume(SubmittedReportInfo(SubmissionStatus.NEW_ISSUE))
                onSuccess(project, sentryId)
            } else {
                consumer.consume(SubmittedReportInfo(SubmissionStatus.FAILED))
            }
        }

        return true
    }

    private fun onSuccess(project: Project?, sentryId: SentryId) {
        val message = "The report was sent successfully, your unique error number: '$sentryId'"

        AdmNotification(message)
            .withTitle("Sending Report")
            .withActions(AdmNotification.Action("Copy error ID") { _, notification ->
                MyUtils.copyToClipboard(sentryId.toString())
                notification.expire()
            })
            .show(project)
    }
}
