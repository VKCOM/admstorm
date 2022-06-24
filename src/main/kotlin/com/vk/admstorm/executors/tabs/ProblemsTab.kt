package com.vk.admstorm.executors.tabs

import com.intellij.icons.AllIcons
import com.intellij.util.ui.JBUI
import com.vk.admstorm.executors.SimpleComponentWithActions
import com.vk.admstorm.utils.MyUiUtils

class ProblemsTab(name: String = "Problems") : BaseTab(name) {
    var panel = JBUI.Panels.simplePanel()

    override fun componentWithActions() = SimpleComponentWithActions(panel, panel)

    override fun componentToFocus() = panel

    override fun icon() = MyUiUtils.createLayeredIcon(AllIcons.Toolwindows.ToolWindowProblemsEmpty, verticalShift = 1)

    override fun clear() {
        panel = JBUI.Panels.simplePanel()
    }
}
