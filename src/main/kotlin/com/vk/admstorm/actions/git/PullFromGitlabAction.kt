package com.vk.admstorm.actions.git

import com.intellij.execution.Output
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.vk.admstorm.actions.AdmActionBase
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.git.sync.SyncChecker
import com.vk.admstorm.git.sync.conflicts.GitPullProblemsHandler
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.ssh.LostConnectionHandler
import com.vk.admstorm.utils.MyUtils.runBackground
import com.vk.admstorm.utils.ServerNameProvider
import com.vk.admstorm.utils.extensions.pluginEnabled

class PullFromGitlabAction : AdmActionBase() {
    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        pullAction(e.project!!)
    }

    private fun pullAction(project: Project) {
        SyncChecker(project).doCheckSyncSilentlyTask({
            onCanceledSync()
        }) {
            doPullFromGitlabTask(project) {
                doPullToLocalTask(project)
            }
        }
    }

    private fun onCanceledSync() {
        AdmWarningNotification("Pull Gitlab → ${ServerNameProvider.name()} → Local was canceled due to unresolved sync issues")
            .withTitle("Pull Canceled")
            .withActions(
                AdmNotification.Action("Resolve issues...") { e, notification ->
                    notification.expire()
                    SyncChecker.getInstance(e.project!!).doCheckSyncSilentlyTask(null) {
                        pullAction(e.project!!)
                    }
                })
            .show()
    }

    companion object {
        /**
         * Starts task for pulling from Gitlab to remote.
         */
        private fun doPullFromGitlabTask(project: Project, onReady: Runnable) {
            runBackground(project, "Pull from Gitlab to ${ServerNameProvider.name()}") { indicator ->
                indicator.isIndeterminate = false
                doPullFromGitlab(project, indicator, onReady)
            }
        }

        private fun doPullFromGitlab(project: Project, indicator: ProgressIndicator, onReady: Runnable?) {
            val currentBranch = GitUtils.remoteCurrentBranch(project)
            val output = GitUtils.remotePullFromGitlab(project, currentBranch, indicator)
            if (output.exitCode != 0) {
                if (LostConnectionHandler.handle(project, output) {
                        doPullFromGitlab(project, indicator, onReady)
                    }) return

                GitPullProblemsHandler(project).handle(output, currentBranch, {
                    doRemoteStashAndPull(project, indicator, onReady)
                }, {
                    doRemoteForcePull(project, indicator, onReady)
                })
                return
            }

            showNotification(output, "Gitlab", ServerNameProvider.name())
            onReady?.run()
        }

        private fun doRemoteForcePull(project: Project, indicator: ProgressIndicator, onReady: Runnable?) {
            GitUtils.remoteDropFiles(project)
            doPullFromGitlab(project, indicator, onReady)
        }

        private fun doRemoteStashAndPull(project: Project, indicator: ProgressIndicator, onReady: Runnable?) {
            GitUtils.remoteStashAndAction(project) {
                doPullFromGitlab(project, indicator, onReady)
            }
        }

        /**
         * Starts task for pulling from remote to local.
         */
        fun doPullToLocalTask(project: Project, onReady: Runnable? = null) {
            runBackground(project, "Pull from ${ServerNameProvider.name()} to local") { indicator ->
                indicator.isIndeterminate = false
                doPullToLocal(project, indicator, onReady)
            }
        }

        private fun doPullToLocal(project: Project, indicator: ProgressIndicator, onReady: Runnable? = null) {
            val currentRemoteBranch = GitUtils.remoteCurrentBranch(project)
            val output = GitUtils.pullFromServer(project, currentRemoteBranch, indicator)
            if (output.exitCode != 0) {
                if (LostConnectionHandler.handle(project, output) {
                        doPullToLocalTask(project)
                    }) return

                GitPullProblemsHandler(project).handle(output, currentRemoteBranch, {
                    doLocalStashAndPull(project, indicator, onReady)
                }, {
                    doLocalForcePull(project, indicator, onReady)
                })
                return
            }

            showNotification(output, ServerNameProvider.name(), "local")
            onReady?.run()
        }

        private fun doLocalForcePull(project: Project, indicator: ProgressIndicator, onReady: Runnable?) {
            GitUtils.localDropFiles(project)
            doPullToLocal(project, indicator, onReady)
        }

        private fun doLocalStashAndPull(project: Project, indicator: ProgressIndicator, onReady: Runnable?) {
            GitUtils.localStashAndAction(project) {
                doPullToLocal(project, indicator, onReady)
            }
        }

        private fun showNotification(output: Output, from: String, to: String) {
            val alreadyUpToDate = output.stdout.contains("Already up to date") ||
                    output.stdout.contains("Уже обновлено")

            val message = output.stdout.split("\n").stream().filter {
                it.contains("insertions") || it.contains("deletions") ||
                        it.contains("insertion") || it.contains("changed") ||
                        it.contains("добавлено") || it.contains("удалено") ||
                        it.contains("добавлен") || it.contains("изменен")
            }.findFirst().orElse("")

            val title =
                if (alreadyUpToDate) "All files on $to are up-to-date"
                else "Successful pull on $to from $from"

            AdmNotification(message)
                .withTitle(title)
                .show()
        }
    }

    override fun update(e: AnActionEvent) {
        if (e.project == null || !e.project!!.pluginEnabled()) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        e.presentation.text = "Pull Gitlab → ${ServerNameProvider.uppercase()} → Local"
    }
}
