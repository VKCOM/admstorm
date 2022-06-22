package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.services.SentryService
import com.vk.admstorm.ui.MyIcons
import com.vk.admstorm.utils.MyUtils

class SendLogsToSentryAction : AdmActionBase() {
    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val sentryId = SentryService.getInstance(project).sendIdeaLog()

        AdmNotification("Thanks for feedback! Your unique feedback number: '$sentryId'")
            .withTitle("Logs successfully sent to Sentry")
            .withActions(AdmNotification.Action("Copy feedback ID") { _, notification ->
                MyUtils.copyToClipboard(sentryId.toString())
                notification.expire()
            })
            .show(project)
    }

    override fun beforeUpdate(e: AnActionEvent) {
        e.presentation.icon = MyIcons.sentry
    }
}
