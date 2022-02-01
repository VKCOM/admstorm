package com.vk.admstorm.notifications

import com.intellij.notification.NotificationType

class AdmWarningNotification(content: String = "", important: Boolean = false) :
    AdmNotification(content, NotificationType.WARNING, important)
