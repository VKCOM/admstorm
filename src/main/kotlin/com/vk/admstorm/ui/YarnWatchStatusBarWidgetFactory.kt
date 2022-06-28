package com.vk.admstorm.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.vk.admstorm.utils.extensions.pluginEnabled

class YarnWatchStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId() = YarnWatchStatusBarWidget.WIDGET_ID

    override fun getDisplayName() = "Yarn Watch"

    override fun isAvailable(project: Project) = project.pluginEnabled()

    override fun createWidget(project: Project) = YarnWatchStatusBarWidget(project)

    override fun canBeEnabledOn(statusBar: StatusBar) = true

    override fun disposeWidget(widget: StatusBarWidget) {
        Disposer.dispose(widget)
    }
}
