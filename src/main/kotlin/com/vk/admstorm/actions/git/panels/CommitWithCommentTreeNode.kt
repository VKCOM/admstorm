package com.vk.admstorm.actions.git.panels

import com.intellij.dvcs.push.ui.PushLogTreeUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.*
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import com.vk.admstorm.git.sync.commits.Commit
import java.awt.Color

class CommitWithCommentTreeNode(
    project: Project,
    commit: Commit,
    private val myComment: String,
    private val myFgColor: JBColor = JBColor(0x00b53d, 0x6ba65d),
    private val myBgColor: JBColor = JBColor(0xebfcf1, 0x313b32)
) : CommitTreeNode(project, commit) {

    companion object {
        private val LABEL_SELECTION_FG = UIUtil.getTreeSelectionForeground(true)
        private val LABEL_SELECTION_BG: Color = JBColor(
            ColorUtil.toAlpha(LABEL_SELECTION_FG, 20),
            ColorUtil.toAlpha(LABEL_SELECTION_FG, 30)
        )
        private val LABEL_FONT = RelativeFont.TINY.small()
    }

    private var lastIsSelected: Boolean = false
    private var textIcon: TextIcon? = null

    override fun render(renderer: ColoredTreeCellRenderer) {
        val isSelected = renderer.tree.isPathSelected(TreeUtil.getPathFromRoot(this))

        val repositoryDetailsTextAttributes =
            PushLogTreeUtil.addTransparencyIfNeeded(renderer, SimpleTextAttributes.REGULAR_ATTRIBUTES, isChecked())
        renderer.append(commit.subject, repositoryDetailsTextAttributes)

        val icon = if (textIcon != null && lastIsSelected == isSelected) {
            textIcon
        } else {
            lastIsSelected = isSelected
            val textIcon =
                TextIcon(myComment, myFgColor, myBgColor, 0)

            textIcon.setInsets(JBUI.insets(2, 6, 2, 2))
            textIcon.setRound(JBUIScale.scale(4))
            textIcon.setFont(LABEL_FONT.derive(renderer.font))
            textIcon.setForeground(if (isSelected) LABEL_SELECTION_FG else myFgColor)
            textIcon.setBackground(if (isSelected) LABEL_SELECTION_BG else myBgColor)
            textIcon
        }

        renderer.isIconOnTheRight = true
        renderer.icon = icon
    }
}
