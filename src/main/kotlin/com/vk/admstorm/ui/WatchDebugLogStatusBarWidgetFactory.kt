package com.vk.admstorm.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory
import com.vk.admstorm.utils.extensions.pluginEnabled

class WatchDebugLogStatusBarWidgetFactory : StatusBarEditorBasedWidgetFactory() {
    override fun getId(): String = WatchDebugLogStatusBarWidget.WIDGET_ID

    override fun getDisplayName() = "Watch Debug Log"

    override fun isAvailable(project: Project) = project.pluginEnabled()

    override fun createWidget(project: Project) = WatchDebugLogStatusBarWidget(project)

    override fun disposeWidget(widget: StatusBarWidget) {
        Disposer.dispose(widget)
    }
}
