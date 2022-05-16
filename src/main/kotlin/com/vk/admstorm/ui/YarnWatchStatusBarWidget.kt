package com.vk.admstorm.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import com.intellij.openapi.wm.impl.status.TextPanel
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.ExperimentalUI
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.vk.admstorm.YarnWatchService
import org.jetbrains.annotations.Nls
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel

class YarnWatchStatusBarWidget(project: Project) : EditorBasedStatusBarPopup(project, true) {
    private var myPanel: YarnWatchStatusBarPanel? = null
    private val service = YarnWatchService.getInstance(project)

    private val animatedErrorIcon = AnimatedIcon(object : AnimatedIcon.Frame {
        override fun getIcon(): Icon = MyIcons.yarnWatchError
        override fun getDelay(): Int = 600
    }, object : AnimatedIcon.Frame {
        override fun getIcon(): Icon = MyIcons.yarnWatchStopped
        override fun getDelay(): Int = 600
    })

    override fun getWidgetState(file: VirtualFile?): WidgetState {
        return when (YarnWatchService.getInstance(project).state()) {
            YarnWatchService.State.RUNNING -> {
                MyWidgetState("Yarn watch works", "yarn watch", MyIcons.yarnWatchWorking)
            }
            YarnWatchService.State.STOPPED -> {
                MyWidgetState("Yawn watch is stopped", "yarn watch", MyIcons.yarnWatchStopped)
            }
            YarnWatchService.State.WITH_ERRORS -> {
                MyWidgetState("Yarn watch works, but found errors", "yarn watch", animatedErrorIcon)
            }
        }
    }

    override fun createPopup(context: DataContext): ListPopup? {
        val state = getWidgetState(null)
        if (state !is MyWidgetState || editor == null) {
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

    override fun createInstance(project: Project): StatusBarWidget {
        return YarnWatchStatusBarWidget(project)
    }

    override fun ID(): String {
        return WIDGET_ID
    }

    private class MyWidgetState(toolTip: String, text: String, icon: Icon) : WidgetState(toolTip, text, true) {
        init {
            this.icon = icon
        }
    }

    override fun createComponent(): JPanel {
        myPanel = YarnWatchStatusBarPanel()
        return myPanel as YarnWatchStatusBarPanel
    }

    override fun updateComponent(state: WidgetState) {
        myPanel!!.setIcon(state.icon)
        myPanel!!.setText(state.text)
        myPanel!!.toolTipText = state.toolTip
    }

    override fun isEmpty() = StringUtil.isEmpty(myPanel!!.text)

    class YarnWatchStatusBarPanel : JPanel() {
        private val myLabel: TextPanel
        private val myIconLabel: JLabel

        init {
            isOpaque = false
            layout = BoxLayout(this, BoxLayout.LINE_AXIS)
            alignmentY = CENTER_ALIGNMENT
            myLabel = object : TextPanel() {}
            myLabel.setFont(if (SystemInfo.isMac) JBUI.Fonts.label(11f) else JBFont.label())
            add(myLabel)
            border = JBUI.Borders.empty()
            myIconLabel = JLabel("")
            if (!ExperimentalUI.isNewUI()) {
                myIconLabel.border = JBUI.Borders.empty(2, 2, 2, 0)
            }
            add(myIconLabel)
        }

        fun setText(text: @Nls String) {
            myLabel.text = text
        }

        val text: String?
            get() = myLabel.text

        fun setIcon(icon: Icon?) {
            myIconLabel.icon = icon
            myIconLabel.isVisible = icon != null
        }
    }

    companion object {
        val WIDGET_ID: String = YarnWatchStatusBarWidget::class.java.name
    }
}
