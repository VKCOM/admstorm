package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.services.HastebinService
import com.vk.admstorm.ui.AdmIcons
import com.vk.admstorm.utils.MyUtils
import com.vk.admstorm.utils.MyUtils.readIdeaLogFile

class SendLogsToHastebinAction : AdmActionBase() {
    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val hasteLink = HastebinService.getInstance(project).createHaste(readIdeaLogFile())

        if (hasteLink == null) {
            AdmNotification()
                .withTitle("Hastebin service unavailable. Try again later")
                .show(project)
            return
        }

        AdmNotification("Thanks for logs!")
            .withTitle("Logs successfully sent to Hastebin")
            .withActions(AdmNotification.Action("Copy hastebin link") { _, notification ->
                MyUtils.copyToClipboard(hasteLink)
                notification.expire()
            })
            .show(project)
    }

    override fun beforeUpdate(e: AnActionEvent) {
        e.presentation.icon = AdmIcons.Service.Hastebin
    }
}
