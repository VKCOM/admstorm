package com.vk.admstorm.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import com.intellij.util.messages.MessageBusConnection
import com.vk.admstorm.services.WatchDebugLogService
import com.vk.admstorm.services.WatchDebugLogService.WatchDebugLogListener
import com.vk.admstorm.utils.extensions.pluginEnabled
import javax.swing.Icon
import javax.swing.JPanel

class WatchDebugLogStatusBarWidget(project: Project) : EditorBasedStatusBarPopup(project, true) {
    companion object {
        private val WIDGET_ID: String = WatchDebugLogStatusBarWidget::class.java.name
    }

    private var panel: ToolsStatusBarPanel? = null
    private val service = WatchDebugLogService.getInstance(project)

    override fun getWidgetState(file: VirtualFile?): WidgetState {
        return when (WatchDebugLogService.getInstance(project).state()) {
            WatchDebugLogService.State.RUNNING -> {
                DebugLogWidgetState("Watch debug log works", "watch debug log", AdmIcons.General.ToolWorking)
            }

            WatchDebugLogService.State.STOPPED -> {
                DebugLogWidgetState("Watch debug log is stopped", "watch debug log", AdmIcons.General.ToolStopped)
            }
        }
    }

    override fun createPopup(context: DataContext): ListPopup? {
        val state = getWidgetState(null)
        if (state !is DebugLogWidgetState) {
            return null
        }

        val actionGroup = object : ActionGroup() {
            override fun getChildren(e: AnActionEvent?) = actions()
        }

        return JBPopupFactory.getInstance().createActionGroupPopup(
            "Watch Debug Log", actionGroup, context,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false
        )
    }

    private fun actions(): Array<AnAction> {
        val stopAction = object : AnAction("Stop", "Stop watch debug log", AllIcons.Actions.Suspend) {
            override fun actionPerformed(e: AnActionEvent) {
                service.stop()
            }
        }
        return when (service.state()) {
            WatchDebugLogService.State.RUNNING -> {
                arrayOf(
                    stopAction,
                    object : AnAction("Open Console", "Open console", AllIcons.Debugger.Console) {
                        override fun actionPerformed(e: AnActionEvent) {
                            service.showConsole()
                        }
                    },
                )
            }

            WatchDebugLogService.State.STOPPED -> {
                arrayOf(
                    object : AnAction("Start", "Start watch debug log", AllIcons.Actions.Execute) {
                        override fun actionPerformed(e: AnActionEvent) {
                            service.start()
                        }
                    },
                )
            }
        }
    }

    override fun registerCustomListeners(connection: MessageBusConnection) {
        connection.subscribe(WatchDebugLogListener.TOPIC, object : WatchDebugLogListener {
            override fun onUpdateState() {
                update()
            }
        })
    }

    override fun createInstance(project: Project) = WatchDebugLogStatusBarWidget(project)

    override fun ID() = WIDGET_ID

    private class DebugLogWidgetState(toolTip: String, text: String, icon: Icon) : WidgetState(toolTip, text, true) {
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

        panel?.setIcon(state.icon)
        panel?.setText(state.text!!)
        panel?.toolTipText = state.toolTip
    }

    override val isEmpty: Boolean
        get() = panel?.text.isNullOrEmpty()
}
