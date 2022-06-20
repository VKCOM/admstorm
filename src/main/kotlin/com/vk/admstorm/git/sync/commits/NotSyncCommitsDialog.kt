package com.vk.admstorm.git.sync.commits

import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.vk.admstorm.actions.git.PullFromGitlabAction
import com.vk.admstorm.actions.git.PushToGitlabAction
import com.vk.admstorm.actions.git.panels.CommitWithCommentTreeNode
import com.vk.admstorm.actions.git.panels.LocalRepoTreeNode
import com.vk.admstorm.actions.git.panels.PushCommitsPanel
import com.vk.admstorm.utils.MyUiUtils
import com.vk.admstorm.utils.ServerNameProvider
import git4idea.DialogManager
import git4idea.branch.GitBranchUtil
import java.awt.Insets
import javax.swing.*
import javax.swing.border.Border

class NotSyncCommitsDialog(
    private val project: Project,
    private val options: Options,
) : DialogWrapper(project, true) {

    companion object {
        private val LOG = logger<NotSyncCommitsDialog>()

        private const val CENTER_PANEL_HEIGHT = 400
        private const val CENTER_PANEL_WIDTH = 800

        fun show(project: Project, options: Options, onSync: Runnable): Choice {
            val dialog = NotSyncCommitsDialog(project, options)
            DialogManager.show(dialog)

            val choice = Choice.fromDialogExitCode(dialog.exitCode)

            val force = when (choice) {
                Choice.SYNC -> true
                Choice.CANCEL -> return choice
            }

            if (options.needPushToServer) {
                doPushNewCommitsToServerTask(project, force, onSync)
            } else {
                doPullNewCommitsFromServerTask(project, onSync)
            }

            return choice
        }

        private fun doPullNewCommitsFromServerTask(project: Project, onSyncFinish: Runnable?) {
            PullFromGitlabAction.doPullToLocalTask(project, onSyncFinish)
        }

        private fun doPushNewCommitsToServerTask(project: Project, force: Boolean, onSyncFinish: Runnable?) {
            PushToGitlabAction.doForcePushOrNotToServerTask(project, force, onSyncFinish)
        }
    }

    enum class Choice {
        SYNC, CANCEL;

        companion object {
            fun fromDialogExitCode(exitCode: Int) = when (exitCode) {
                OK_EXIT_CODE -> SYNC
                CANCEL_EXIT_CODE -> CANCEL
                else -> {
                    LOG.error("Unexpected exit code: $exitCode")
                    CANCEL
                }
            }
        }
    }

    data class Options(
        val localCommit: Commit,
        val remoteCommit: Commit,
        val betweenCommits: List<Commit>,
        val countBetween: Int,
        val needPushToServer: Boolean,
    )

    private var myPushCommitsPanel: PushCommitsPanel

    private fun createMessageTextPane(): JTextPane {
        val messageText = if (options.needPushToServer)
            """<html>
            There are <b>${options.countBetween} commits</b> in <b>local</b> branch 
            that were not pushed to <b>${ServerNameProvider.name()}</b> branch
            </html>
            """.trimIndent()
        else
            """<html>
            There are <b>${options.countBetween} commits</b> in <b>${ServerNameProvider.name()}</b> branch 
            that were not fetched to <b>local</b> branch
            </html>
            """.trimIndent()

        return MyUiUtils.createTextInfoComponent(messageText)
    }

    private fun createInfoMessageTextPane(): JTextPane {
        val messageText = if (options.needPushToServer)
            """<html>
            Synchronization will push <b>local commits</b> to the <b>${ServerNameProvider.name()}</b> branch
            </html>
            """.trimIndent()
        else
            """<html>
            Synchronization will fetch <b>${ServerNameProvider.name()} commits</b> to the <b>local</b> branch
            </html>
            """.trimIndent()

        return MyUiUtils.createTextInfoComponent(messageText)
    }

    init {
        title = "Commits not Synchronized"

        val commitBuilder = { commit: Commit ->
            when {
                commit == options.remoteCommit && options.needPushToServer -> {
                    CommitWithCommentTreeNode(
                        project, commit,
                        "remote HEAD",
                        JBColor(0xCA7045, 0xCA8564),
                        JBColor(0xFCF2EB, 0x3B332E)
                    )
                }
                commit == options.localCommit && !options.needPushToServer -> {
                    CommitWithCommentTreeNode(
                        project, commit,
                        "local HEAD",
                        JBColor(0xCA7045, 0xCA8564),
                        JBColor(0xFCF2EB, 0x3B332E)
                    )
                }
                else -> CommitWithCommentTreeNode(
                    project, commit,
                    if (options.needPushToServer) "new local" else "new remote"
                )
            }
        }

        val rootBuilder = {
            val branchName = GitBranchUtil.getCurrentRepository(project)?.currentBranch?.name!!
            LocalRepoTreeNode(branchName)
        }

        val commits = mutableListOf<Commit>()
        commits.addAll(options.betweenCommits)
        if (options.needPushToServer) {
            commits.add(options.remoteCommit)
        } else {
            commits.add(options.localCommit)
        }

        myPushCommitsPanel = PushCommitsPanel(project, commits, options.countBetween, commitBuilder, rootBuilder)

        okAction.putValue(Action.SHORT_DESCRIPTION, "Sync all changes")
        cancelAction.putValue(Action.SHORT_DESCRIPTION, "Don't sync")

        setOKButtonText("Sync")

        init()
    }

    override fun getPreferredFocusedComponent() = myPushCommitsPanel.getPreferredFocusedComponent()

    override fun createContentPaneBorder(): Border? = null

    override fun createSouthPanel(): JComponent? {
        return super.createSouthPanel().apply {
            border = JBUI.Borders.empty(8, 12)
        }
    }

    override fun createSouthAdditionalPanel(): JPanel {
        val panel = JPanel()
        panel.layout = GridLayoutManager(
            1, 1,
            Insets(8, 0, 0, 0), -1, -1
        )

        panel.add(createInfoMessageTextPane(), GridConstraints().apply {
            row = 0; column = 0; fill = GridConstraints.FILL_BOTH
        })

        val iconPanel = JPanel()
        iconPanel.layout = GridLayoutManager(
            1, 1,
            Insets(3, 0, 0, 5), -1, -1
        )

        val label = JLabel()
        label.icon = AllIcons.General.BalloonInformation

        iconPanel.add(label, GridConstraints().apply {
            row = 0; column = 0; fill = GridConstraints.FILL_BOTH
        })

        return JBUI.Panels.simplePanel(2, 0)
            .addToCenter(panel)
            .addToLeft(iconPanel)
    }

    override fun createCenterPanel(): JComponent {
        val bottomPanel = JPanel()
        bottomPanel.layout = GridLayoutManager(
            1, 1,
            Insets(15, 12, 0, 12), -1, -1
        )

        bottomPanel.add(createMessageTextPane(), GridConstraints().apply {
            row = 0; column = 0; fill = GridConstraints.FILL_BOTH
        })

        val panel = JBUI.Panels.simplePanel(2, 0)
            .addToCenter(myPushCommitsPanel)
            .addToBottom(bottomPanel)

        myPushCommitsPanel.preferredSize = JBDimension(CENTER_PANEL_WIDTH, CENTER_PANEL_HEIGHT)

        return panel
    }
}
