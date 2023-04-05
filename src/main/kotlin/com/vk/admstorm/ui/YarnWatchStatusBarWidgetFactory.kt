package com.vk.admstorm.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.vk.admstorm.env.EnvListener
import com.vk.admstorm.utils.extensions.pluginIsInitialized
import com.vk.admstorm.utils.extensions.pluginEnabled

class YarnWatchStatusBarWidgetFactory : StatusBarWidgetFactory {
    companion object {
        const val FACTORY_ID = "yarnWatchStatusBar"
    }

    override fun getId() = FACTORY_ID

    override fun getDisplayName() = "Yarn Watch"

    override fun isAvailable(project: Project): Boolean {
        if (!project.pluginIsInitialized()) {
            return false
        }

        return project.pluginEnabled()
    }

    override fun createWidget(project: Project) = YarnWatchStatusBarWidget(project)

    override fun canBeEnabledOn(statusBar: StatusBar) = true

    internal class Listener(private val project: Project) : EnvListener {
        override fun onResolve() {
            project.service<StatusBarWidgetsManager>().updateWidget(YarnWatchStatusBarWidgetFactory::class.java)
        }
    }
}
