package com.vk.admstorm.git

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.BranchChangeListener
import com.vk.admstorm.AdmStartupService
import com.vk.admstorm.git.sync.branches.RemoteBranchSwitcher
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.settings.AdmStormSettingsState
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.utils.ServerNameProvider
import com.vk.admstorm.utils.extensions.pluginEnabled
import git4idea.branch.GitBrancher
import git4idea.repo.GitRepositoryManager
import git4idea.util.GitUIUtil

/**
 * Responsible for synchronizing branches between local and remote repositories.
 */
class AdmBranchContextTracker(private var myProject: Project) : BranchChangeListener {
    companion object {
        private val LOG = logger<AdmBranchContextTracker>()
    }

    private var myPreviousBranch = ""

    override fun branchWillChange(branchName: String) {
        myPreviousBranch = branchName
    }

    override fun branchHasChanged(branchName: String) {
        if (!AdmStormSettingsState.getInstance().needSyncBranchCheckout || !myProject.pluginEnabled()) {
            return
        }

        val onGitConflictCanceled = {
            doRollback()
        }

        if (!SshConnectionService.getInstance(myProject).isConnected()) {
            AdmWarningNotification(
                """
                    There was no connection during the switch, so the branch on ${ServerNameProvider.name()} was not changed
                    <br>
                    Try restarting ${GitUIUtil.code("ssh-agent")} and reconnect
                """.trimIndent()
            )
                .withTitle("Branch is not switched on ${ServerNameProvider.name()}")
                .withActions(AdmNotification.Action("Reconnect and switch...") { _, notification ->
                    notification.expire()

                    SshConnectionService.getInstance(myProject).connect {
                        AdmStartupService.getInstance(myProject).afterConnectionTasks {
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
