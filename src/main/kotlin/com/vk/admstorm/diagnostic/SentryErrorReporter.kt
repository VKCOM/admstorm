package com.vk.admstorm.diagnostic

import com.intellij.diagnostic.IdeaReportingEvent
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.diagnostic.SubmittedReportInfo.SubmissionStatus
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.Consumer
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.services.SentryService
import com.vk.admstorm.utils.MyUtils
import io.sentry.protocol.SentryId
import java.awt.Component

class SentryErrorReporter : ErrorReportSubmitter() {
    companion object {
        private val LOG = logger<SentryErrorReporter>()
    }

    override fun getReportActionText(): String = "Report to Sentry"

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?, parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>
    ): Boolean {
        val project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(parentComponent))
        if (project == null) {
            LOG.info("Cannot get project to submit exception to Sentry")
            consumer.consume(SubmittedReportInfo(SubmissionStatus.FAILED))
            return false
        }

        val sentry = SentryService.getInstance(project)
        for (event in events) {
            if (event !is IdeaReportingEvent) {
                continue
            }

            val sentryId = sentry.sendError(event.data.throwable)
            return if (sentryId != SentryId.EMPTY_ID) {
                consumer.consume(SubmittedReportInfo(SubmissionStatus.NEW_ISSUE))
                onSuccess(project, sentryId)
                true
            } else {
                LOG.info("An error occurred when sending an error to Sentry")
                consumer.consume(SubmittedReportInfo(SubmissionStatus.FAILED))
                false
            }
        }

        return true
    }

    private fun onSuccess(project: Project, sentryId: SentryId) {
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
