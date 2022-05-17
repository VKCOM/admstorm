package com.vk.admstorm.actions.git.panels

import com.intellij.dvcs.push.ui.PushLogTreeUtil
import com.intellij.dvcs.push.ui.TooltipNode
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes

open class ShowMoreCommitsNode(private val remainCount: Int) : TreeBaseNode(), TooltipNode {
    override fun render(renderer: ColoredTreeCellRenderer) {
        val repositoryDetailsTextAttributes =
            PushLogTreeUtil.addTransparencyIfNeeded(renderer, SimpleTextAttributes.REGULAR_ATTRIBUTES, isChecked())
        renderer.append("...", repositoryDetailsTextAttributes)
    }

    override fun getTooltip(): String {
        return "Not shown commits ($remainCount)"
    }
}
