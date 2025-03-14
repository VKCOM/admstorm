package com.vk.admstorm.actions.git.commit

import com.intellij.dvcs.commit.getCommitAndPushActionName
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.CommitSession
import com.intellij.openapi.vcs.changes.LocalCommitExecutor
import com.intellij.openapi.vcs.changes.actions.BaseCommitExecutorAction
import com.intellij.vcs.commit.CommitWorkflowHandlerState
import com.intellij.vcs.commit.commitProperty
import com.vk.admstorm.utils.ServerNameProvider
import com.vk.admstorm.utils.extensions.pluginEnabled

private val IS_PUSH_TO_GITLAB_AFTER_COMMIT_KEY = Key.create<Boolean>("Git.Commit.IsPushToGitlabAfterCommit")
internal var CommitContext.isPushToGitlabAfterCommit: Boolean by commitProperty(IS_PUSH_TO_GITLAB_AFTER_COMMIT_KEY)

class GitCommitAndPushToGitlabExecutor : LocalCommitExecutor() {
    companion object {
        const val ID = "Vcs.RunCommitAndPushToGitlab.Executor"
    }

    override fun getId(): String = ID

    override fun getActionText(): @NlsActions.ActionText String =
        "Commit and Push → ${ServerNameProvider.name()} → Gitlab…"

    override fun useDefaultAction(): Boolean = false

    override fun createCommitSession(commitContext: CommitContext): CommitSession {
        commitContext.isPushToGitlabAfterCommit = true
        return CommitSession.VCS_COMMIT
    }

    override fun getHelpId(): String? = null
}

class GitCommitAndPushToAdmExecutorAction : BaseCommitExecutorAction() {
    override val executorId: String get() = GitCommitAndPushToGitlabExecutor.ID

    override fun update(e: AnActionEvent) {
        val workflowHandler = e.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER)
        if (workflowHandler != null) {
            val state = CommitWorkflowHandlerState(
                isAmend = workflowHandler.amendCommitHandler.isAmendCommitMode, isSkipCommitChecks = false
            )
            val templateText = getCommitAndPushActionName(state)

            e.presentation.text =
                templateText.removeSuffix("…") + " → ${ServerNameProvider.name()} → Gitlab…"
        }

        e.presentation.isEnabledAndVisible = e.project != null && e.project!!.pluginEnabled()
    }
}
