package com.vk.admstorm.git.sync

import com.intellij.execution.Output
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.env.Env
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.git.sync.branches.SyncBranchesDialog
import com.vk.admstorm.git.sync.commits.Commit
import com.vk.admstorm.git.sync.commits.GitCommitComparison
import com.vk.admstorm.git.sync.files.FilesNotSyncDialog
import com.vk.admstorm.git.sync.files.GitStatus
import com.vk.admstorm.git.sync.files.RemoteFile
import com.vk.admstorm.utils.MyUtils.measureTime
import com.vk.admstorm.utils.MyUtils.measureTimeValue
import com.vk.admstorm.utils.MyUtils.runBackground
import com.vk.admstorm.utils.ServerNameProvider
import java.util.function.Consumer

@Service
class SyncChecker(private var myProject: Project) {
    companion object {
        fun getInstance(project: Project) = project.service<SyncChecker>()

        private val LOG = logger<SyncChecker>()
        const val FILE_SEPARATOR = "<<<--->>>"
        const val PART_SEPARATOR = "<--->"
    }

    private var myLocalBranch: String = ""
    private var myLocalCommit: Commit? = null
    private var myRemoteBranch: String = ""
    private var myRemoteCommit: Commit? = null

    private var myCommitsDistance = 0
    private var myDifferingFiles = mutableListOf<RemoteFile>()

    enum class State {
        BranchNotSync,
        LastCommitNotSync,
        FilesNotSync,
        Ok,

        Unknown
    }

    fun getDiffFiles() = myDifferingFiles

    /**
     * Returns the current sync state of the repositories.
     *
     * If there is no SSH connection, a [State.Unknown] is returned.
     *
     * @param notCheckFiles if true, file synchronization is not checked
     */
    fun currentState(notCheckFiles: Boolean = false): State {
        val repo = GitUtils.getCurrentRepository(myProject) ?: return State.Ok
        myLocalBranch = repo.currentBranch?.name ?: return State.Ok

        val localLastCommit = GitUtils.localLastCommit(myProject)

        val statusFiles = if (notCheckFiles) emptyList() else GitStatus.localStatus(myProject)
        val statusFilesJson = GitStatus.asJson(statusFiles)

        val command = "${Env.data.syncScriptCommand} $myLocalBranch ${localLastCommit.hash} '$statusFilesJson'"

        val output = CommandRunner.runRemotely(myProject, command)
        if (output.exitCode == CommandRunner.FAILED_CODE) {
            return State.Unknown
        }

        if (output.stderr.isNotEmpty()) {
            LOG.warn("Output stderr for sync checker is not empty: '${output.stderr}'")
        }

        if (output.exitCode == 0) {
            // If the files from the local git status match, then if the git
            // status is not empty on the server, then all files from it are
            // returned in the output.
            if (output.stdout.isNotEmpty()) {
                myDifferingFiles.clear()

                output.stdout
                    .split(FILE_SEPARATOR)
                    .filter { it.isNotEmpty() }
                    .forEach {
                        val remoteFile = RemoteFile.create(myProject, it) ?: return@forEach

                        if (!remoteFile.equalWithLocal()) {
                            myDifferingFiles.add(remoteFile)
                        }
                    }

                if (myDifferingFiles.isNotEmpty()) {
                    return State.FilesNotSync
                }
            }

            return State.Ok
        }

        return when {
            output.stdout.contains("branch mismatch") -> {
                handleBranchMismatch(output)
            }
            output.stdout.contains("commit mismatch") -> {
                handleCommitMismatch(output, localLastCommit)
            }
            output.stdout.contains("files mismatch") -> {
                handleFilesMismatch(output)
            }
            else -> State.Ok
        }
    }

    private fun handleBranchMismatch(output: Output): State {
        val parts = output.stdout.split(PART_SEPARATOR)
        myRemoteBranch = parts[1].trim()
        return State.BranchNotSync
    }

    private fun handleCommitMismatch(
        output: Output,
        localLastCommit: Commit
    ): State {
        val parts = output.stdout.split(PART_SEPARATOR)
        myCommitsDistance = parts[1].trim().toIntOrNull() ?: 0
        myRemoteCommit = Commit.fromString(parts[2]).also { it.fromRemote = true }
        myLocalCommit = localLastCommit

        if (myCommitsDistance == -2) {
            myCommitsDistance = GitUtils.localDistanceBetweenCommits(myProject, myRemoteCommit!!, myLocalCommit!!)
        }
        return State.LastCommitNotSync
    }

    private fun handleFilesMismatch(output: Output): State {
        myDifferingFiles.clear()

        output.stdout
            .removePrefix("files mismatch\n")
            .split(FILE_SEPARATOR)
            .filter { it.isNotEmpty() }
            .forEach {
                val file = RemoteFile.create(myProject, it) ?: return@forEach
                if (file.equalWithLocal()) return@forEach

                // If a file with the same name has already been added, then we
                // don't need to add it again to avoid problems when resolving
                // the conflict further.
                if (myDifferingFiles.find { diffFile -> diffFile.path == file.path } != null)
                    return@forEach

                myDifferingFiles.add(file)
            }

        return State.FilesNotSync
    }

    /**
     * Handles the state received from the [currentState] function.
     *
     * If the state describes an out of sync branch, then after this is resolved,
     * another sync check will be run to check that the commits are also in sync.
     *
     * @param onCancelSync block that will be executed if the state describes
     *                     an out of sync, and it has not been resolved
     * @param onSync block that will be executed if the state describes a situation when the
     *               repositories are synchronized or the desynchronization has been resolved
     */
    private fun handleState(state: State, onCancelSync: Runnable?, onSync: Runnable) {
        if (state == State.Ok) {
            onSync.run()
            return
        }

        invokeLater {
            if (state == State.BranchNotSync) {
                val dialog = SyncBranchesDialog(myProject, myLocalBranch, myRemoteBranch, {
                    onCancelSync?.run()
                }) {
                    invokeLater invokeLater2@{
                        val afterCheckoutState = measureTimeValue(LOG, "get current sync state") {
                            currentState()
                        }
                        if (afterCheckoutState == State.Unknown) {
                            LOG.warn("No SSH connection")
                            return@invokeLater2
                        }

                        if (afterCheckoutState != State.Ok) {
                            handleState(afterCheckoutState, onCancelSync, onSync)
                        } else {
                            onSync.run()
                        }
                    }
                }

                val ok = dialog.showAndGet()
                if (!ok) {
                    onCancelSync?.run()
                }
                return@invokeLater
            }

            if (state == State.LastCommitNotSync) {
                val checker = GitCommitComparison(myProject, {
                    onCancelSync?.run()
                }) {
                    onSync.run()
                }

                runBackground(myProject, "Calculate commits difference") {
                    checker.compare(myRemoteCommit!!, myLocalCommit!!, myCommitsDistance)
                }
                return@invokeLater
            }

            if (state == State.FilesNotSync) {
                val dialog = FilesNotSyncDialog(myProject, myDifferingFiles)

                val ok = dialog.showAndGet()
                if (ok) {
                    onSync.run()
                    return@invokeLater
                }

                onCancelSync?.run()
                return@invokeLater
            }
        }
    }

    fun doCheckSyncSilentlyTask(onCancelSync: Runnable?, onSync: Runnable) {
        runBackground(myProject, "AdmStorm: Check sync with ${ServerNameProvider.name()}") {
            val state = measureTimeValue(LOG, "get current sync state") {
                currentState()
            }

            if (state == State.Unknown) {
                LOG.warn("No SSH connection")
                return@runBackground
            }

            measureTime(LOG, "handle current sync state") {
                handleState(state, onCancelSync, onSync)
            }
        }
    }

    fun doOnlyBranchAndCommitsCheckSyncTask(onCancelSync: Runnable?, onSync: Consumer<Boolean>) {
        runBackground(myProject, "Check branch and commits sync") {
            val state = currentState(notCheckFiles = true)
            if (state == State.Unknown) {
                LOG.warn("No SSH connection")
                return@runBackground
            }

            if (state == State.BranchNotSync) {
                handleState(state, onCancelSync) {
                    onSync.accept(true)
                }
                return@runBackground
            }

            if (state == State.LastCommitNotSync) {
                onSync.accept(true)
                return@runBackground
            }

            invokeLater {
                onSync.accept(false)
            }
        }
    }
}
