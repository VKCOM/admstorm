package com.vk.admstorm.git.sync.conflicts

import com.intellij.execution.Output
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.vk.admstorm.git.GitConflictResolutionStrategy
import com.vk.admstorm.settings.AdmStormSettingsState
import com.vk.admstorm.ui.MessageDialog
import git4idea.util.GitUIUtil.bold

class GitPullProblemsHandler(project: Project) : GitProblemsHandlerBase(project) {
    companion object {
        private val LOG = logger<GitCheckoutProblemsHandler>()

        private const val NO_TRACKING_INFO = "no tracking information"
        private const val REMOTE_REF_NOT_FOUND = "Couldn't find remote ref"
        private const val PULL_WITHOUT_RECONCILE = "Pulling without specifying how to reconcile divergent"
        private const val NEED_COMMIT_OR_STASH = "Please commit your changes or stash them before"
        private const val LOCAL_CHANGES_OVERWRITE = "Your local changes to the following"
    }

    fun handle(output: Output, branchName: String, stashAndAction: Runnable, forceAction: Runnable) {
        val stderr = output.stderr
        if (stderr.isNotEmpty()) {
            LOG.warn("Git error appear: $stderr")
        }

        when {
            stderr.contains(NO_TRACKING_INFO) -> {
                handlePullBranchWithoutTrackingInformation(branchName)
            }
            stderr.contains(REMOTE_REF_NOT_FOUND) -> {
                handlePullBranchWithoutTrackingInformation(branchName)
            }
            stderr.contains(PULL_WITHOUT_RECONCILE) -> {
                handlePossiblyFileOverwrite(output, stashAndAction, forceAction)
            }
            stderr.contains(NEED_COMMIT_OR_STASH) || stderr.contains(LOCAL_CHANGES_OVERWRITE) -> {
                handlePossiblyFileOverwrite(output, stashAndAction, forceAction)
            }
            else -> {
                showGenericWarningDialog(output)
            }
        }
    }

    private fun handlePossiblyFileOverwrite(output: Output, stashAndAction: Runnable, forceAction: Runnable): State {
        if (output.exitCode == 0) return State.Ok

        when (AdmStormSettingsState.getInstance().checkoutConflictResolutionStrategy) {
            GitConflictResolutionStrategy.Stash -> stashAndAction.run()
            GitConflictResolutionStrategy.ForceCheckout -> forceAction.run()
            else -> return showFilesDialog(output, "pull", stashAndAction, forceAction)
        }

        return State.Handled
    }

    private fun handlePullBranchWithoutTrackingInformation(branchName: String) {
        val message = """
            It seems that the ${bold(branchName)} branch does not have tracking information. 
            
            Push the branch to the Gitlab first to be able to git pull it.
        """.trimIndent()

        MessageDialog.showWarning(message, "No Tracking Information")
    }
}
