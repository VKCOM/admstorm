package com.vk.admstorm.git

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.BranchChangeListener
import com.vk.admstorm.AdmStormStartupActivity
import com.vk.admstorm.env.Env
import com.vk.admstorm.git.sync.branches.RemoteBranchSwitcher
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.settings.AdmStormSettingsState
import com.vk.admstorm.ssh.SshConnectionService
import git4idea.branch.GitBrancher
import git4idea.repo.GitRepositoryManager
import git4idea.util.GitUIUtil

/**
 * Responsible for synchronizing branches between local and remote repositories.
 */
class AdmBranchContextTracker(private var myProject: Project) : BranchChangeListener {
    companion object {
        private val LOG = Logger.getInstance(AdmBranchContextTracker::class.java)
    }

    private var myPreviousBranch = ""

    override fun branchWillChange(branchName: String) {
        myPreviousBranch = branchName
    }

    override fun branchHasChanged(branchName: String) {
        if (!AdmStormSettingsState.getInstance().needSyncBranchCheckout) {
            return
        }

        val onGitConflictCanceled = {
            doRollback()
        }

        if (!SshConnectionService.getInstance(myProject).isConnected()) {
            AdmWarningNotification(
                """
                    There was no connection during the switch, so the branch on ${Env.data.serverName} was not changed
                    <br>
                    Try restarting ${GitUIUtil.code("ssh-agent")} and reconnect
                """.trimIndent()
            )
                .withTitle("Branch is not switched on ${Env.data.serverName.ifEmpty { "dev-server" }}")
                .withActions(AdmNotification.Action("Reconnect and switch...") { _, notification ->
                    notification.expire()

                    SshConnectionService.getInstance(myProject).connect {
                        AdmStormStartupActivity.getInstance(myProject).afterConnectionTasks(myProject) {
                            RemoteBranchSwitcher(myProject, onGitConflictCanceled)
                                .switch(branchName, false)
                        }
                    }
                })
                .show()
            return
        }

        RemoteBranchSwitcher(myProject, onGitConflictCanceled)
            .switch(branchName, false)
    }

    private fun doRollback() {
        if (myPreviousBranch == "") {
            LOG.warn("Previous branch is undefined, it is not possible to revert to the previous state.")
            return
        }
        val repos = GitRepositoryManager.getInstance(myProject).repositories
        GitBrancher.getInstance(myProject).checkout(myPreviousBranch, false, repos, null)
        myPreviousBranch = ""
    }
}
