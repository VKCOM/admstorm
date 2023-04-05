package com.vk.admstorm.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetSettings
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager

object StatusBarUtils {
    private val LOG = logger<StatusBarUtils>()

    fun setEnabled(project: Project, id: String, value: Boolean) {
        val manager = project.service<StatusBarWidgetsManager>()
        val factory = manager.getWidgetFactoryById(id) ?: return

        ApplicationManager.getApplication()
            .getService(StatusBarWidgetSettings::class.java)
            .setEnabled(factory, value)
        manager.updateWidget(factory)
    }

    fun getEnabled(project: Project, id: String): Boolean {
        val manager = project.service<StatusBarWidgetsManager>()
        val factory = manager.getWidgetFactoryById(id) ?: return false

        return ApplicationManager.getApplication()
            .getService(StatusBarWidgetSettings::class.java)
            .isEnabled(factory)
    }

    private fun StatusBarWidgetsManager.getWidgetFactoryById(id: String): StatusBarWidgetFactory? {
        val factory = getWidgetFactories().find { it.id == id }
        if (factory == null) {
            LOG.warn("Factory by id: `$id` not found")
        }

        return factory
    }
}
