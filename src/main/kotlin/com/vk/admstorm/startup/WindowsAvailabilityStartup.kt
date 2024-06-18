package com.vk.admstorm.startup

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.vk.admstorm.notifications.AdmErrorNotification
import com.vk.admstorm.notifications.AdmNotification

object WindowsAvailabilityStartup {
    fun checkAvailability(project: Project) {
        //TODO: remove it after release AdmStorm on Windows
        if (SystemInfo.isWindows) {
            val properties = PropertiesComponent.getInstance(project)

            if (!properties.getBoolean("windowsSupportNotification")) {
                AdmErrorNotification("AdmStorm is not available on Windows yet").withActions(
                    AdmNotification.Action("Turn off this notification") { _, notification ->
                        properties.setValue("windowsSupportNotification", true)
                        notification.expire()
                    }
                ).show()
            }
        }
    }
}
