package com.vk.admstorm.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.notification.impl.NotificationFullContent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
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
), NotificationFullContent {

    companion object {
        const val ID = "AdmStorm"
        const val IMPORTANT_ID = "AdmStorm Important"
        private val LOG = Logger.getInstance(AdmNotification::class.java)
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

    fun show() {
        invokeLater {
            Notifications.Bus.notify(this)
            LOG.info("Notification: title: $title, content: ${content.ifEmpty { "<empty>" }}, type: $type")
        }
    }

    class Action(msg: String, private val runnable: BiConsumer<AnActionEvent, Notification>) :
        NotificationAction(msg) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            runnable.accept(e, notification)
        }
    }
}
