package com.vk.admstorm.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetSettings
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager

object StatusBarUtils {
    fun setEnabled(project: Project, id: String, value: Boolean) {
        val manager = project.service<StatusBarWidgetsManager>()
        val factory = manager.widgetFactories
            .find { it.id == id } ?: return
        ApplicationManager.getApplication().getService(StatusBarWidgetSettings::class.java)
            .setEnabled(factory, value)
        manager.updateWidget(factory)
    }

    fun getEnabled(project: Project, id: String): Boolean {
        val manager = project.service<StatusBarWidgetsManager>()
        val factory = manager.widgetFactories
            .find { it.id == id } ?: return false
        return ApplicationManager.getApplication().getService(StatusBarWidgetSettings::class.java)
            .isEnabled(factory)
    }
}
