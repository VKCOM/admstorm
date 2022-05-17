package com.vk.admstorm.git.sync.commits

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.ui.MessageDialog
import com.vk.admstorm.utils.ServerNameProvider

class GitCommitComparison(
    private var myProject: Project,
    private var myOnCancelSync: Runnable?,
    private var myOnSync: Runnable,
) {
    fun compare(remoteCommit: Commit, localCommit: Commit, distance: Int) {
        if (localCommit.committerDate > remoteCommit.committerDate) {
            remoteCommitBeforeLocal(localCommit, remoteCommit, distance)
        }

        if (localCommit.committerDate < remoteCommit.committerDate) {
            localCommitBeforeRemote(localCommit, remoteCommit, distance)
        }
    }

    private fun remoteCommitBeforeLocal(localCommit: Commit, remoteCommit: Commit, distance: Int) {
        if (distance == -1) {
            doIfInvalidDistance()
            return
        }

        val (commitsBetween, countBetween) = GitUtils.localCommitsInRange(myProject, remoteCommit, localCommit)

        showDialog(localCommit, remoteCommit, commitsBetween, countBetween, true)
    }

    private fun localCommitBeforeRemote(localCommit: Commit, remoteCommit: Commit, distance: Int) {
        if (distance == -1) {
            doIfInvalidDistance()
            return
        }

        val (commitsBetween, countBetween) = GitUtils.remoteCommitsInRange(myProject, localCommit, remoteCommit)

        showDialog(localCommit, remoteCommit, commitsBetween, countBetween, false)
    }

    private fun showDialog(
        localCommit: Commit,
        remoteCommit: Commit,
        commitsBetween: List<Commit>,
        countBetween: Int,
        needPushToServer: Boolean,
    ) {
        invokeLater {
            val choice =
                NotSyncCommitsDialog.show(
                    myProject,
                    localCommit,
                    remoteCommit,
                    commitsBetween,
                    countBetween,
                    needPushToServer,
                    myOnSync
                )

            if (choice == NotSyncCommitsDialog.Choice.CANCEL) {
                myOnCancelSync?.run()
            }
        }
    }

    private fun doIfInvalidDistance() {
        MessageDialog.showWarning(
            """
                Commits on the ${ServerNameProvider.name()} and local are not sequential, most likely there are commits on the ${ServerNameProvider.name()} and on the local created from the same parent.
                
                Plugin cannot automatically resolve this. Before proceeding, you need to resolve this conflict manually.
            """.trimIndent(),
            "Commits not Synchronized", myProject
        )

        myOnCancelSync?.run()
    }
}
