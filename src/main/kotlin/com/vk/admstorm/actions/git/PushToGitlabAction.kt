package com.vk.admstorm.actions.git

import com.intellij.execution.Output
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.encoding.EncodingRegistry
import com.vk.admstorm.AdmService
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.actions.AdmActionBase
import com.vk.admstorm.env.Env
import com.vk.admstorm.executors.PushToRemoteExecutor
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.git.sync.SyncChecker
import com.vk.admstorm.git.sync.files.GitStatus
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.settings.AdmStormSettingsState
import com.vk.admstorm.ssh.LostConnectionHandler
import com.vk.admstorm.ui.MessageDialog
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MyUtils.measureTimeValue
import com.vk.admstorm.utils.MyUtils.runBackground
import com.vk.admstorm.utils.ServerNameProvider
import git4idea.DialogManager
import git4idea.branch.GitBranchUtil
import git4idea.util.GitUIUtil.code

class PushToGitlabAction : AdmActionBase() {
    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        runAction(e.project!!)
    }

    fun runAction(project: Project) {
        SyncChecker(project).doOnlyBranchAndCommitsCheckSyncTask({
            onCanceledSync()
        }) { needPushToServer ->
            pushAction(project, needPushToServer)
        }
    }

    private fun pushAction(project: Project, needPushToServer: Boolean) {
        if (needPushToServer) {
            doForcePushOrNotToServerTask(project, true) {
                showDialog(project)
            }
            return
        }

        showDialog(project)
    }

    private fun showDialog(project: Project) {
        runBackground(project, "Preparing data for dialog") {
            val branchName = GitBranchUtil.getCurrentRepository(project)?.currentBranch?.name!!
            val branchExistsInGitlab = GitUtils.remoteBranchExist(project, "origin/$branchName")

            val commits = if (branchExistsInGitlab)
                GitUtils.newServerCommits(project, branchName)
            else
                GitUtils.remoteCommitsSinceMaster(project)

            invokeLater {
                val dialog =
                    PushOptionsDialog(project, "Push Commits to Gitlab from ${ServerNameProvider.name()}", commits)
                DialogManager.show(dialog)
                val choice = PushOptionsDialog.Choice.fromDialogExitCode(dialog.exitCode)

                val force = when (choice) {
                    PushOptionsDialog.Choice.PUSH -> false
                    PushOptionsDialog.Choice.FORCE -> true
                    PushOptionsDialog.Choice.CANCEL -> return@invokeLater
                }

                val options = PushOptions(force, dialog.pushTagMode(), dialog.additionalParameters())
                doPushToGitlabTask(project, options)
            }
        }
    }

    companion object {
        private val LOG = Logger.getInstance(PushToGitlabAction::class.java)

        private fun doPushToGitlabTask(project: Project, options: PushOptions) {
            runBackground(project, "Push from ${ServerNameProvider.name()} to Gitlab") {
                doPushToGitlab(project, options)
            }
        }

        /**
         * Checks for the presence of at least one file in git status (including
         * untracked files) on the development server, and in this case shows
         * a dialog in which user can either stash files or discard changes.
         *
         * This check is necessary, since the push will always fail if there is
         * at least one file in the status.
         *
         * @return true if git status is empty or the user has selected one of
         *         the two suggested options to remove files from git status.
         */
        private fun checkGitStatus(project: Project): Boolean {
            val repo = GitBranchUtil.getCurrentRepository(project) ?: return true
            val root = repo.root
            val statusFiles = GitStatus.rawRemoteStatus(project, root, includeUntracked = true)
                .filter {
                    it.path.virtualFile == null || !it.path.virtualFile!!.isDirectory
                }

            if (statusFiles.isEmpty()) {
                return true
            }

            val filesString = statusFiles.joinToString(separator = ";") { file ->
                val encoding = EncodingRegistry.getInstance().getEncoding(file.path.virtualFile, true)
                val encodingString = if (encoding == null) ":unknown" else ":${encoding.displayName()}"

                return@joinToString MyPathUtils.remotePathByLocalPath(project, file.path.path) + encodingString
            }

            val output =
                CommandRunner.runRemotely(project, "${Env.data.syncScriptCommand} files_content '$filesString'")

            val filesContents = output.stdout.split(SyncChecker.FileSeparator)

            val dialog = GitStatusFilesDialog(project, statusFiles, filesContents)
            val res = dialog.showAndGet()
            if (!res) {
                return false
            }

            if (dialog.isDiscardChangesSelected()) {
                return doDiscardChangesTask(project)
            }

            return doStashTask(project)
        }

        private fun doDiscardChangesTask(project: Project): Boolean {
            return ProgressManager.getInstance().run(object : Task.WithResult<Boolean, Exception>(
                project,
                "Discard ${ServerNameProvider.name()} changes",
                true
            ) {
                override fun compute(indicator: ProgressIndicator): Boolean {
                    val output = GitUtils.remoteStashAndDrop(project)
                    if (output.exitCode != 0) {
                        MessageDialog.showError(
                            """
                                Unable to execute ${code("git stash save -u 'drop stash' && git stash drop")}
                                
                                ${output.stderr}
                            """.trimIndent(),
                            "Discard ${ServerNameProvider.name()} Changes Failed"
                        )
                        return false
                    }

                    return true
                }
            })
        }

        private fun doStashTask(project: Project): Boolean {
            return ProgressManager.getInstance().run(object : Task.WithResult<Boolean, Exception>(
                project,
                "Stash ${ServerNameProvider.name()} changes",
                true
            ) {
                override fun compute(indicator: ProgressIndicator): Boolean {
                    val output = GitUtils.remoteStash(project)
                    if (output.exitCode != 0) {
                        MessageDialog.showError(
                            """
                                Unable to execute ${code("git stash")}
                                
                                ${output.stderr}
                            """.trimIndent(),
                            "Stash ${ServerNameProvider.name()} Failed"
                        )
                        return false
                    }

                    return true
                }
            })
        }

        private fun doPushToGitlab(project: Project, options: PushOptions) {
            invokeLater {
                LOG.info("Run check git status")

                val canPush = checkGitStatus(project)
                if (!canPush) {
                    AdmWarningNotification("Push to Gitlab is not possible because git status is not empty")
                        .withTitle("Push canceled")
                        .withActions(
                            AdmNotification.Action("Resolve and push...") { _, notification ->
                                notification.expire()
                                invokeLater {
                                    doPushToGitlabTask(project, options)
                                }
                            }
                        )
                        .show()
                    return@invokeLater
                }

                val currentBranch = GitUtils.remoteCurrentBranch(project)
                val newCommits = GitUtils.countNewServerCommits(project, currentBranch)

                val title = when (newCommits) {
                    -1, 0 -> "Successful pushed to<br>origin/$currentBranch"
                    1 -> "Pushed 1 commit to<br>origin/$currentBranch"
                    else -> "Pushed $newCommits commits to<br>origin/$currentBranch"
                }

                val env = if (AdmStormSettingsState.getInstance().runPhpLinterAsInTeamcityWhenPushToGitlab) {
                    "PHP_LINTER_FULL=1 "
                } else ""

                val command = mutableListOf("git", "push").apply {
                    if (options.force) add("--force")
                    if (options.tagMode != null) add(options.tagMode.argument ?: "")
                    if (options.additionalParameters.isNotBlank()) add(options.additionalParameters)
                    add("origin")
                    add(currentBranch)
                }.joinToString(" ")

                LOG.info("Run push command '$command' with env '$env'")
                val executor = PushToRemoteExecutor(project, "$env$command")

                executor.withOutputHandler { output, _ ->
                    LOG.info("Run 'git push' output handler")

                    if (output.exitCode != 0) {
                        AdmWarningNotification()
                            .withTitle("Failed push to Gitlab")
                            .show()
                        LOG.info("Push to gitlab failed")
                        return@withOutputHandler
                    }

                    val alreadyUpToDate = output.stderr.contains("Everything up-to-date")

                    val linkToMergeRequest = getMergeRequestFromOutput(output)

                    val actions = if (linkToMergeRequest != null) arrayOf(
                        AdmNotification.Action("Create Merge Request") { _, notification ->
                            notification.expire()
                            BrowserUtil.browse(linkToMergeRequest)
                        }
                    ) else emptyArray()

                    val preciseTitle =
                        if (alreadyUpToDate) "Everything up-to-date"
                        else title

                    AdmNotification()
                        .withTitle(preciseTitle)
                        .withActions(*actions)
                        .show()

                    LOG.info("Push to gitlab done")
                }

                executor.run()
            }
        }

        private fun getMergeRequestFromOutput(output: Output): String? {
            var linkToMergeRequestIndex = -1
            val stderrLines = output.stderr.split("\n")

            stderrLines.forEachIndexed { index, line ->
                if (line.contains("To create a merge request")) {
                    linkToMergeRequestIndex = index + 1
                }
            }

            return if (linkToMergeRequestIndex != -1)
                stderrLines[linkToMergeRequestIndex]
                    .removePrefix("remote:")
                    .trim()
            else null
        }

        fun doForcePushOrNotToServerTask(project: Project, force: Boolean, after: Runnable? = null) {
            if (force) {
                doForcePushToServerTask(project, after)
                return
            }

            doPushToServerTask(project, after)
        }

        private fun doPushToServerTask(project: Project, after: Runnable? = null) {
            runBackground(project, "Push to ${ServerNameProvider.name()}") { indicator ->
                indicator.text2 = "Waiting SSH connection"

                val ok = measureTimeValue(LOG, "push to ${ServerNameProvider.name()}") {
                    doPushToServer(project, force = false, null, indicator)
                }

                if (ok) {
                    after?.run()
                }
            }
        }

        private fun doForcePushToServerTask(project: Project, after: Runnable? = null) {
            runBackground(project, "Force push to ${ServerNameProvider.name()}") { indicator ->
                indicator.text2 = "Waiting SSH connection"

                val ok = measureTimeValue(LOG, "force push to ${ServerNameProvider.name()}") {
                    doPushToServer(project, force = true, after, indicator)
                }

                if (ok) {
                    after?.run()
                }
            }
        }

        private fun doPushToServer(
            project: Project,
            force: Boolean = false,
            after: Runnable? = null,
            indicator: ProgressIndicator
        ): Boolean {
            val currentBranch = GitBranchUtil.getCurrentRepository(project)!!.currentBranch!!.name
            LOG.info("Run push to ${ServerNameProvider.name()} (current branch: '$currentBranch')")

            val countNewCommits = GitUtils.countNewLocalCommits(project, currentBranch)
            LOG.info("Count new commits: $countNewCommits")

            val title = when (countNewCommits) {
                -1, 0 -> "Everything up-to-date"
                1 -> "Pushed 1 commit to<br>${ServerNameProvider.name()}/$currentBranch"
                else -> "Pushed $countNewCommits commits to<br>${ServerNameProvider.name()}/$currentBranch"
            }

            val output = GitUtils.pushToServer(project, currentBranch, force, indicator)
            if (output.exitCode != 0) {
                if (LostConnectionHandler.handle(project, output) {
                        doForcePushOrNotToServerTask(project, force, after)
                    }) return false

                MessageDialog.showWarning(
                    """
                        It looks like git is having trouble pushing the current state to ${ServerNameProvider.name()}. 
                        
                        Try using ${code("Force Push")}.
                    """.trimIndent(),
                    "Problem with Push to ${ServerNameProvider.name()}"
                )

                LOG.warn("Git Push to ${ServerNameProvider.name()} Failed\n" + output.stderr)
                return false
            }

            GitUtils.remoteDropFiles(project)

            AdmNotification()
                .withTitle(title)
                .show()

            LOG.info("Push to ${ServerNameProvider.name()} done")
            return true
        }
    }

    private fun onCanceledSync() {
        AdmWarningNotification(
            "Push Local → ${ServerNameProvider.name()} → Gitlab was canceled due to unresolved sync issues"
        )
            .withTitle("Push canceled")
            .withActions(
                AdmNotification.Action("Resolve issues...") { e, notification ->
                    notification.expire()
                    SyncChecker.getInstance(e.project!!)
                        .doOnlyBranchAndCommitsCheckSyncTask(null) { needPushToServer ->
                            pushAction(e.project!!, needPushToServer)
                        }
                }
            )
            .show()
    }

    override fun update(e: AnActionEvent) {
        if (e.project == null || !AdmService.getInstance(e.project!!).needBeEnabled()) {
            e.presentation.isEnabledAndVisible = false
        }

        e.presentation.text = "Push Local → ${ServerNameProvider.uppercase()} → Gitlab"
    }
}
