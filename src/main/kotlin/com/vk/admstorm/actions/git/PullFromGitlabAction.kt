package com.vk.admstorm.actions.git

import com.intellij.execution.Output
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.vk.admstorm.AdmService
import com.vk.admstorm.actions.AdmActionBase
import com.vk.admstorm.env.Env
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.git.sync.GitErrorHandler
import com.vk.admstorm.git.sync.SyncChecker
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.ssh.LostConnectionHandler
import com.vk.admstorm.ui.MessageDialog
import com.vk.admstorm.utils.MyUtils.runBackground

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
        AdmWarningNotification("Pull Gitlab → ${Env.data.serverName} → Local was canceled due to unresolved sync issues")
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
        private fun doPullFromGitlabTask(project: Project, after: Runnable) {
            runBackground(project, "Pull from Gitlab to ${Env.data.serverName}") { indicator ->
                indicator.isIndeterminate = false
                val ok = doPullFromGitlab(project, indicator)
                if (ok) {
                    after.run()
                }
            }
        }

        fun doPullToLocalTask(project: Project, after: Runnable? = null) {
            runBackground(project, "Pull from ${Env.data.serverName} to local") { indicator ->
                indicator.isIndeterminate = false
                doPullToLocal(project, indicator)
                after?.run()
            }
        }

        private fun doPullFromGitlab(project: Project, indicator: ProgressIndicator): Boolean {
            val currentBranch = GitUtils.remoteCurrentBranch(project)
            val output = GitUtils.remotePullFromGitlab(project, currentBranch, indicator)
            if (output.exitCode != 0) {
                GitErrorHandler(project).handlePull(output, currentBranch)
                return false
            }

            showNotification(output, "Gitlab", Env.data.serverName)
            return true
        }

        private fun doPullToLocal(project: Project, indicator: ProgressIndicator) {
            val currentBranch = GitUtils.remoteCurrentBranch(project)
            val output = GitUtils.pullFromServer(project, currentBranch, indicator)
            if (output.exitCode != 0) {
                if (LostConnectionHandler.handle(project, output) {
                        doPullToLocalTask(project)
                    }) return

                MessageDialog.showWarning(output.stderr, "Git Pull to Local from ${Env.data.serverName} Failed")
                return
            }

            showNotification(output, Env.data.serverName, "local")
        }

        private fun showNotification(output: Output, from: String, to: String) {
            val alreadyUpToDate = output.stdout.contains("Already up to date")
                    || output.stdout.contains("Уже обновлено")

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
        if (e.project == null || !AdmService.getInstance(e.project!!).needBeEnabled()) {
            e.presentation.isEnabledAndVisible = false
        }

        e.presentation.text = "Pull Gitlab → ${Env.data.serverName.replaceFirstChar { it.uppercaseChar() }} → Local"
    }
}
