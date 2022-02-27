package com.vk.admstorm.actions.git.panels

import com.intellij.dvcs.push.ui.PushLogTreeUtil
import com.intellij.ui.*
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import com.vk.admstorm.utils.ServerNameProvider
import java.awt.Color

class ServerRepoTreeNode(
    private val myCurrentBranch: String,
    private val isNewBranch: Boolean
) : TreeBaseNode() {

    companion object {
        private val NEW_BRANCH_LABEL_SELECTION_FG = UIUtil.getTreeSelectionForeground(false)
        private val NEW_BRANCH_LABEL_SELECTION_BG: Color = JBColor(
            ColorUtil.toAlpha(NEW_BRANCH_LABEL_SELECTION_FG, 20),
            ColorUtil.toAlpha(NEW_BRANCH_LABEL_SELECTION_FG, 30)
        )
        private val NEW_BRANCH_LABEL_FONT = RelativeFont.TINY.small()
        private const val SEPARATOR = " : "

        private val NEW_BRANCH_FG_COLOR = JBColor(0x00b53d, 0x6ba65d)
        private val NEW_BRANCH_BG_COLOR = JBColor(0xebfcf1, 0x313b32)
    }

    fun getCurrentBranchName(): String = myCurrentBranch

    override fun render(renderer: ColoredTreeCellRenderer) {
        val isSelected = renderer.tree.isPathSelected(TreeUtil.getPathFromRoot(this))

        val repositoryDetailsTextAttributes =
            PushLogTreeUtil.addTransparencyIfNeeded(renderer, SimpleTextAttributes.REGULAR_ATTRIBUTES, isChecked())
        renderer.append(ServerNameProvider.name(), SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES)
        renderer.append(SEPARATOR, repositoryDetailsTextAttributes)
        renderer.append(myCurrentBranch)
        renderer.append(" " + UIUtil.rightArrow() + " ", repositoryDetailsTextAttributes)
        renderer.append("gitlab", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES)
        renderer.append(SEPARATOR, repositoryDetailsTextAttributes)
        renderer.append(myCurrentBranch)

        if (isNewBranch) {
            val textIcon =
                TextIcon("new", NEW_BRANCH_FG_COLOR, NEW_BRANCH_BG_COLOR, 0)

            renderer.isIconOnTheRight = true
            textIcon.setInsets(JBUI.insets(2, 6, 2, 2))
            textIcon.setRound(JBUIScale.scale(4))
            textIcon.setFont(NEW_BRANCH_LABEL_FONT.derive(renderer.font))
            textIcon.setForeground(if (isSelected) NEW_BRANCH_LABEL_SELECTION_FG else NEW_BRANCH_FG_COLOR)
            textIcon.setBackground(if (isSelected) NEW_BRANCH_LABEL_SELECTION_BG else NEW_BRANCH_BG_COLOR)
            renderer.icon = textIcon
        }
    }
}
