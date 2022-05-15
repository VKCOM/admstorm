package com.vk.admstorm.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory

class YarnWatchStatusBarWidgetFactory : StatusBarEditorBasedWidgetFactory() {
    override fun getId(): String = YarnWatchStatusBarWidget.WIDGET_ID

    override fun getDisplayName(): String {
        return "Yarn Watch"
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return YarnWatchStatusBarWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        Disposer.dispose(widget)
    }
}
