package com.vk.admstorm.configuration.problems.actions

import com.intellij.ide.IdeTooltip
import com.intellij.ide.IdeTooltipManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.components.panels.NonOpaquePanel
import com.vk.admstorm.configuration.phplinter.PhpLinterCheckers
import com.vk.admstorm.configuration.problems.panels.ProblemTreeNode
import com.vk.admstorm.utils.MyUiUtils
import java.awt.FlowLayout
import java.awt.Point
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.border.EmptyBorder

class ShowProblemDescriptionAction : DumbAwareAction() {
    /**
     * Show a tooltip with the passed [text] in the upper left corner
     * of the passed [component].
     * This tooltip will automatically disappear if you move the mouse.
     * In text, you can use HTML to mark up elements.
     */
    private fun showTooltip(text: String, component: JComponent) {
        val panel = NonOpaquePanel(FlowLayout(FlowLayout.CENTER, 0, 0))
        panel.border = EmptyBorder(10, 10, 10, 10)
        panel.add(MyUiUtils.createTextInfoComponent(text))

        val tooltip = object : IdeTooltip(component, Point(0, 0), panel) {
            override fun canBeDismissedOnTimeout() = false
        }

        IdeTooltipManager.getInstance().show(tooltip, true)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val data = getSelectedNode(e) as? ProblemTreeNode ?: return
        val problem = data.problem

        val description = PhpLinterCheckers.nameToCheckerDoc[problem.name] ?: "no description"

        showTooltip(description, e.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) as JComponent)
    }

    private fun getSelectedNode(event: AnActionEvent): Any? {
        val tree = event.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) as? JTree
        return tree?.selectionPath?.lastPathComponent
    }
}
