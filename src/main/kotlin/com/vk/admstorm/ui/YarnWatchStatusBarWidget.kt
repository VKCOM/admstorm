package com.vk.admstorm.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import com.intellij.ui.AnimatedIcon
import com.vk.admstorm.services.YarnWatchService
import com.vk.admstorm.utils.extensions.pluginEnabled
import javax.swing.Icon
import javax.swing.JPanel

class YarnWatchStatusBarWidget(project: Project) : EditorBasedStatusBarPopup(project, true) {
    companion object {
        val WIDGET_ID: String = YarnWatchStatusBarWidget::class.java.name
    }

    private var panel: ToolsStatusBarPanel? = null
    private val service = YarnWatchService.getInstance(project)

    private val animatedErrorIcon = AnimatedIcon(600, MyIcons.toolError, MyIcons.toolStopped)

    override fun getWidgetState(file: VirtualFile?): WidgetState {
        return when (YarnWatchService.getInstance(project).state()) {
            YarnWatchService.State.RUNNING -> {
                YarnWidgetState("Yarn watch works", "yarn watch", MyIcons.toolWorking)
            }
            YarnWatchService.State.STOPPED -> {
                YarnWidgetState("Yawn watch is stopped", "yarn watch", MyIcons.toolStopped)
            }
            YarnWatchService.State.WITH_ERRORS -> {
                YarnWidgetState("Yarn watch works, but found errors", "yarn watch", animatedErrorIcon)
            }
        }
    }

    override fun createPopup(context: DataContext): ListPopup? {
        val state = getWidgetState(null)
        if (state !is YarnWidgetState) {
            return null
        }

        val actionGroup = object : ActionGroup() {
            override fun getChildren(e: AnActionEvent?) = actions()
        }

        return JBPopupFactory.getInstance().createActionGroupPopup(
            "Yarn Watch", actionGroup, context,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false
        )
    }

    private fun actions(): Array<AnAction> {
        val stopAction = object : AnAction("Stop", "Stop yarn watch", AllIcons.Actions.Suspend) {
            override fun actionPerformed(e: AnActionEvent) {
                service.stop()
            }
        }
        return when (service.state()) {
            YarnWatchService.State.RUNNING -> {
                arrayOf(
                    stopAction,
                    object : AnAction("Open Console", "Open console", AllIcons.Debugger.Console) {
                        override fun actionPerformed(e: AnActionEvent) {
                            service.showConsole()
                        }
                    },
                )
            }
            YarnWatchService.State.WITH_ERRORS -> {
                arrayOf(
                    stopAction,
                    object : AnAction("Show Errors", "Show errors", AllIcons.Ide.FatalError) {
                        override fun actionPerformed(e: AnActionEvent) {
                            service.showConsole()
                        }
                    },
                )
            }
            YarnWatchService.State.STOPPED -> {
                arrayOf(
                    object : AnAction("Start", "Start yarn watch", AllIcons.Actions.Execute) {
                        override fun actionPerformed(e: AnActionEvent) {
                            service.start()
                        }
                    },
                )
            }
        }
    }

    override fun registerCustomListeners() {
        service.changes.addPropertyChangeListener { evt ->
            if (evt.propertyName == YarnWatchService.PROPERTY_ID) {
                update()
            }
        }
    }

    override fun createInstance(project: Project) = YarnWatchStatusBarWidget(project)

    override fun ID() = WIDGET_ID

    private class YarnWidgetState(toolTip: String, text: String, icon: Icon) : WidgetState(toolTip, text, true) {
        init {
            this.icon = icon
        }
    }

    override fun createComponent(): JPanel {
        panel = ToolsStatusBarPanel()
        return panel as ToolsStatusBarPanel
    }

    override fun updateComponent(state: WidgetState) {
        if (!project.pluginEnabled()) {
            return
        }

        panel!!.setIcon(state.icon)
        panel!!.setText(state.text)
        panel!!.toolTipText = state.toolTip
    }

    override fun isEmpty() = StringUtil.isEmpty(panel!!.text)
}
