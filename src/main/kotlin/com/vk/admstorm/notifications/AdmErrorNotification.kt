package com.vk.admstorm.notifications

import com.intellij.notification.NotificationType

class AdmErrorNotification(content: String = "", important: Boolean = false) :
    AdmNotification(content, NotificationType.ERROR, important)
