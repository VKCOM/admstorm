package com.vk.admstorm.actions.git.panels

import com.intellij.dvcs.push.ui.PushLogTreeUtil
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.UIUtil
import com.vk.admstorm.utils.ServerNameProvider

class LocalRepoTreeNode(private val myCurrentBranch: String) : TreeBaseNode() {
    companion object {
        private const val SEPARATOR = " : "
    }

    fun getCurrentBranchName() = myCurrentBranch

    override fun render(renderer: ColoredTreeCellRenderer) {
        val repositoryDetailsTextAttributes =
            PushLogTreeUtil.addTransparencyIfNeeded(renderer, SimpleTextAttributes.REGULAR_ATTRIBUTES, isChecked())
        renderer.append(myCurrentBranch)
        renderer.append(" ${UIUtil.rightArrow()} ", repositoryDetailsTextAttributes)
        renderer.append(ServerNameProvider.name(), SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES)
        renderer.append(SEPARATOR, repositoryDetailsTextAttributes)
        renderer.append(myCurrentBranch)
    }
}
