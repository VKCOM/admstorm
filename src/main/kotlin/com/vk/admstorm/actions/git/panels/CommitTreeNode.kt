package com.vk.admstorm.actions.git.panels

import com.intellij.dvcs.DvcsUtil
import com.intellij.dvcs.push.ui.PushLogTreeUtil
import com.intellij.dvcs.push.ui.TooltipNode
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.issueLinks.IssueLinkHtmlRenderer
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.vcs.log.VcsCommitMetadata
import com.intellij.vcs.log.ui.frame.CommitPresentationUtil
import com.intellij.vcs.log.util.VcsUserUtil
import com.vk.admstorm.git.sync.commits.Commit

open class CommitTreeNode(
    protected val myProject: Project,
    val commit: Commit,
) : TreeBaseNode(), TooltipNode {

    private val myMetaData: VcsCommitMetadata = commit.asMetaData(myProject)

    fun getPresentation(project: Project): CommitPresentationUtil.CommitPresentation {
        return commit.presentation(project)
    }

    override fun render(renderer: ColoredTreeCellRenderer) {
        val repositoryDetailsTextAttributes =
            PushLogTreeUtil.addTransparencyIfNeeded(renderer, SimpleTextAttributes.REGULAR_ATTRIBUTES, isChecked())
        renderer.append(myMetaData.subject, repositoryDetailsTextAttributes)
    }

    override fun getTooltip(): String {
        val hash = myMetaData.id.toShortString()
        val date = DvcsUtil.getDateString(myMetaData)
        val author = VcsUserUtil.getShortPresentation(myMetaData.author)
        val message = IssueLinkHtmlRenderer.formatTextWithLinks(myProject, myMetaData.fullMessage)
        return DvcsBundle.message(
            "push.commit.node.tooltip.0.hash.1.date.2.author.3.message",
            hash,
            date,
            author,
            message
        )
    }
}
