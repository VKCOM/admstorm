package com.vk.admstorm.configuration.problems.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.vk.admstorm.configuration.problems.panels.ProblemTreeNode
import java.awt.datatransfer.StringSelection
import javax.swing.JTree

class CopyProblemShortDescriptionAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val data = getSelectedNode(e) as? ProblemTreeNode ?: return
        val problem = data.problem
        CopyPasteManager.getInstance().setContents(StringSelection(problem.description))
    }

    private fun getSelectedNode(event: AnActionEvent): Any? {
        val tree = event.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) as? JTree
        return tree?.selectionPath?.lastPathComponent
    }
}
