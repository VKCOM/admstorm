package com.vk.admstorm.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.vk.admstorm.env.EnvListener
import com.vk.admstorm.utils.extensions.pluginEnabled
import com.vk.admstorm.utils.extensions.pluginIsInitialized

class WatchDebugLogStatusBarWidgetFactory : StatusBarWidgetFactory {
    companion object {
        const val FACTORY_ID = "watchDebugLogStatusBar"
    }

    override fun getId() = FACTORY_ID

    override fun getDisplayName() = "Watch Debug Log"

    override fun isAvailable(project: Project): Boolean {
        if (!project.pluginIsInitialized()) {
            return false
        }

        return project.pluginEnabled()
    }
    override fun createWidget(project: Project) = WatchDebugLogStatusBarWidget(project)

    override fun canBeEnabledOn(statusBar: StatusBar) = true

    internal class Listener() : EnvListener {
        override fun onResolve() {
            service<StatusBarWidgetsManager>().updateWidget(WatchDebugLogStatusBarWidgetFactory::class.java)
        }
    }
}
