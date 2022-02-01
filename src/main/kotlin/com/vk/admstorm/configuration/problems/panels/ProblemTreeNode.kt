package com.vk.admstorm.configuration.problems.panels

import com.intellij.dvcs.push.ui.TooltipNode
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.vk.admstorm.actions.git.panels.TreeBaseNode

class ProblemTreeNode(
    val project: Project,
    val problem: Problem,
) : TreeBaseNode(), NavigatableTreeNode, TooltipNode {

    private val myNavigatable: Navigatable?

    init {
        myNavigatable = if (problem.file == null)
            null
        else
            OpenFileDescriptor(project, problem.file, problem.line - 1, 0)
    }

    override fun getNavigatable() = myNavigatable

    override fun render(renderer: ColoredTreeCellRenderer) {
        if (problem.name != null) {
            renderer.append(problem.name)
            renderer.append(": ")
        }
        renderer.append(problem.description)
        renderer.append(" :${problem.line}", SimpleTextAttributes.GRAY_ATTRIBUTES)

        renderer.icon = problem.icon
    }

    override fun getTooltip() = "Double click to go to sources"
}
