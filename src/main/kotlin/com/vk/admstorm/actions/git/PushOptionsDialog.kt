package com.vk.admstorm.actions.git

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.OptionAction
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.vk.admstorm.actions.git.panels.CommitTreeNode
import com.vk.admstorm.actions.git.panels.PushCommitsPanel
import com.vk.admstorm.actions.git.panels.ServerRepoTreeNode
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.git.sync.commits.Commit
import com.vk.admstorm.utils.MyUiUtils.spacer
import git4idea.branch.GitBranchUtil
import git4idea.push.GitPushTagMode
import git4idea.push.localizedTitle
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.border.Border

data class PushOptions(
    val force: Boolean = false,
    val tagMode: GitPushTagMode? = null,
    val additionalParameters: String = "",
)

class PushOptionsDialog(
    project: Project,
    title: String,
    commits: List<Commit>,
) : DialogWrapper(project, true, IdeModalityType.PROJECT) {

    companion object {
        private val LOG = Logger.getInstance(PushOptionsDialog::class.java)

        private const val CENTER_PANEL_HEIGHT = 450
        private const val CENTER_PANEL_WIDTH = 800

        private const val FORCE_EXIT_CODE = NEXT_USER_EXIT_CODE
    }

    enum class Choice {
        PUSH, FORCE, CANCEL;

        companion object {
            fun fromDialogExitCode(exitCode: Int) = when (exitCode) {
                OK_EXIT_CODE -> PUSH
                FORCE_EXIT_CODE -> FORCE
                CANCEL_EXIT_CODE -> CANCEL
                else -> {
                    LOG.error("Unexpected exit code: $exitCode")
                    CANCEL
                }
            }
        }
    }

    private val myPushActions = mutableListOf<Action>()
    private val myPushCommitsPanel: PushCommitsPanel
    private val myAdditionalParametersTextField = ExpandableTextField()
    private val myPushTagsComboBox = ComboBox<GitPushTagMode>()

    init {
        setTitle(title)

        val commitBuilder = { commit: Commit ->
            CommitTreeNode(project, commit)
        }
        val rootNodeBuilder = {
            val branchName = GitBranchUtil.getCurrentRepository(project)?.currentBranch?.name!!
            val exists = GitUtils.remoteBranchExist(project, branchName)
            ServerRepoTreeNode(branchName, !exists)
        }

        myPushCommitsPanel = PushCommitsPanel(project, commits, commitBuilder, rootNodeBuilder)

        myPushActions.add(object : AbstractAction("Push") {
            override fun actionPerformed(e: ActionEvent?) {
                doOKAction()
            }
        })
        myPushActions.add(object : AbstractAction("Force Push") {
            override fun actionPerformed(e: ActionEvent?) {
                close(FORCE_EXIT_CODE)
            }
        })

        myAdditionalParametersTextField.toolTipText = "Additional push parameters"

        init()
    }

    fun additionalParameters() = myAdditionalParametersTextField.text ?: ""
    fun pushTagMode() = when {
        !myPushTagsComboBox.isEnabled -> null
        else -> myPushTagsComboBox.selectedItem as GitPushTagMode
    }

    override fun createActions(): Array<Action> {
        val actions: MutableList<Action> = ArrayList()
        val pushAction = ComplexPushAction(myPushActions[0], myPushActions.subList(1, myPushActions.size))
        pushAction.putValue(DEFAULT_ACTION, java.lang.Boolean.TRUE)
        actions.add(pushAction)
        actions.add(cancelAction)
        return actions.toTypedArray()
    }

    override fun getPreferredFocusedComponent() = myPushCommitsPanel.getPreferredFocusedComponent()
    override fun createContentPaneBorder(): Border? = null

    override fun createSouthPanel(): JComponent? {
        return super.createSouthPanel().apply {
            border = JBUI.Borders.empty(8, 12)
        }
    }

    override fun createSouthAdditionalPanel(): JPanel {
        myAdditionalParametersTextField.minimumSize =
            JBDimension(200, myAdditionalParametersTextField.minimumSize.height)

        val label = JLabel("Push parameters:")
        val additionalParametersPanel = JBUI.Panels.simplePanel(2, 2)
            .addToLeft(label)
            .addToCenter(spacer(5))
            .addToRight(myAdditionalParametersTextField)

        myPushTagsComboBox.isEnabled = false
        myPushTagsComboBox.addItemListener { e: ItemEvent ->
            e.itemSelectable.selectedObjects[0]
        }

        val pushTagsCheckBox = JBCheckBox("Push tags:")
        pushTagsCheckBox.addActionListener {
            myPushTagsComboBox.isEnabled = pushTagsCheckBox.isSelected
        }

        val boxModel = DefaultComboBoxModel<GitPushTagMode>()
        boxModel.addElement(GitPushTagMode.ALL)
        boxModel.addElement(GitPushTagMode.FOLLOW)
        myPushTagsComboBox.renderer = SimpleListCellRenderer.create("", GitPushTagMode::localizedTitle)

        myPushTagsComboBox.model = boxModel
        myPushTagsComboBox.selectedItem = GitPushTagMode.ALL

        val pushTagsPanel = JBUI.Panels.simplePanel(2, 2)
            .addToLeft(pushTagsCheckBox)
            .addToCenter(spacer(5))
            .addToRight(myPushTagsComboBox)

        return JBUI.Panels.simplePanel(2, 2)
            .addToLeft(pushTagsPanel)
            .addToCenter(spacer(20))
            .addToRight(additionalParametersPanel)
    }

    override fun createCenterPanel(): JComponent {
        val panel = JBUI.Panels.simplePanel(0, 2)
            .addToCenter(myPushCommitsPanel)

        myPushCommitsPanel.preferredSize = JBDimension(CENTER_PANEL_WIDTH, CENTER_PANEL_HEIGHT)

        return panel
    }

    private class ComplexPushAction(
        private val myDefaultAction: Action,
        private val myOptions: List<Action>,
    ) : AbstractAction("Push"), OptionAction {

        override fun actionPerformed(e: ActionEvent) {
            myDefaultAction.actionPerformed(e)
        }

        override fun setEnabled(isEnabled: Boolean) {
            super.setEnabled(isEnabled)
            myOptions.forEach {
                it.isEnabled = isEnabled
            }
        }

        override fun getOptions() = myOptions.toTypedArray()
    }
}
