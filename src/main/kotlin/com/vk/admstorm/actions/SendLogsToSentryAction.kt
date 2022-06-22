package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.services.SentryService

class SendLogsToSentryAction : AdmActionBase() {
    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        SentryService.getInstance(project).sendIdeaLog()

        AdmNotification("Thanks for feedback!")
            .withTitle("Logs successfully sent to Sentry")
            .show(project)
    }
}
