package com.vk.admstorm.notifications

import com.intellij.notification.*
import com.intellij.notification.impl.NotificationFullContent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import java.util.function.BiConsumer

open class AdmNotification(
    content: String = "",
    type: NotificationType = NotificationType.INFORMATION,
    important: Boolean = false,
) : Notification(
    if (important) IMPORTANT_ID else ID,
    "AdmStorm",
    content,
    type,
    NotificationListener.URL_OPENING_LISTENER,
), NotificationFullContent {

    companion object {
        const val ID = "AdmStorm"
        const val IMPORTANT_ID = "AdmStorm Important"
        private val LOG = logger<AdmNotification>()
    }

    fun withActions(vararg actions: NotificationAction): AdmNotification {
        actions.forEach {
            addAction(it)
        }

        return this
    }

    fun withTitle(title: String): AdmNotification {
        setTitle(title)
        return this
    }

    fun show(project: Project? = null) {
        invokeLater {
            Notifications.Bus.notify(this, project)
            LOG.info("Notification: title: $title, content: ${content.ifEmpty { "<empty>" }}, type: $type")
        }
    }

    class Action(msg: String, private val runnable: BiConsumer<AnActionEvent, Notification>) : NotificationAction(msg) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            runnable.accept(e, notification)
        }
    }
}
