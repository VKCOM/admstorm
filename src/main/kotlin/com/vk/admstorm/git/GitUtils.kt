package com.vk.admstorm.git

import com.intellij.execution.Output
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.actions.git.listeners.GitPullProgressListener
import com.vk.admstorm.actions.git.listeners.GitPushProgressListener
import com.vk.admstorm.git.sync.commits.Commit
import com.vk.admstorm.ui.MessageDialog
import com.vk.admstorm.utils.MyUtils.runBackground
import com.vk.admstorm.utils.ServerNameProvider
import git4idea.util.GitUIUtil
import java.text.SimpleDateFormat
import java.util.*

/**
 * [GitUtils] class contains all the necessary functionality to work with git,
 * both in the local repository and on the development server.
 */
object GitUtils {
    private val LOG = logger<GitUtils>()

    /**
     * Base command for [pushToServer]
     */
    private const val pushToServerCommand = "git push --tags --progress"

    /**
     * Launches pushing a branch with [branchName] from the local repository to the development server.
     *
     * The progress is displayed in the passed [indicator] (ProgressIndicator).
     *
     * @param force true if you want to push with the --force flag
     * @return Output with [Output.stdout], [Output.stderr] and [Output.exitCode]
     */
    fun pushToServer(
        project: Project,
        branchName: String,
        force: Boolean = false,
        indicator: ProgressIndicator
    ): Output {
        var command = pushToServerCommand
            .withParam(ServerNameProvider.name())
            .withParam(branchName)
        if (force) {
            command = command.withParam("--force")
        }
        return CommandRunner.runLocally(project, command, false, GitPushProgressListener(indicator))
    }

    /**
     * Base command for [pullFromServer]
     */
    private const val pullFromRemoteCommand = "git pull --prune --progress"

    /**
     * Launches pull a branch with [branchName] from the development server to the local repository.
     *
     * The progress is displayed in the passed [indicator] (ProgressIndicator).
     *
     * @return Output with [Output.stdout], [Output.stderr] and [Output.exitCode]
     */
    fun pullFromServer(project: Project, branchName: String, indicator: ProgressIndicator): Output {
        return CommandRunner.runLocally(
            project,
            pullFromRemoteCommand
                .withParam(ServerNameProvider.name())
                .withParam(branchName),
            false,
            GitPullProgressListener(indicator)
        )
    }

    /**
     * Base command for [remotePullFromGitlab]
     */
    private const val pullFromServerCommand = "git pull --prune --progress origin"

    /**
     * Launches pull a branch with [branchName] from Gitlab to the development server.
     *
     * The progress is displayed in the passed [indicator] (ProgressIndicator).
     *
     * @return Output with [Output.stdout], [Output.stderr] and [Output.exitCode]
     */
    fun remotePullFromGitlab(project: Project, branchName: String, indicator: ProgressIndicator): Output {
        return CommandRunner.runRemotely(
            project,
            pullFromServerCommand.withParam(branchName),
            20_000,
            GitPullProgressListener(indicator)
        )
    }

    /**
     * Base command for [remoteCurrentBranch]
     */
    private const val currentBranchCommand = "git rev-parse --abbrev-ref HEAD"

    /**
     * Returns the name of the current branch on the development server.
     */
    fun remoteCurrentBranch(project: Project): String {
        return CommandRunner.runRemotely(project, currentBranchCommand).stdout.trim()
    }

    /**
     * Base command for [localDistanceBetweenCommits]
     */
    private const val commitDistanceCommand = "git rev-list --count"

    /**
     * Returns the distance between commit [first] and [last].
     *
     * If the commits are one after the other, then the return value is 0.
     */
    fun localDistanceBetweenCommits(project: Project, first: Commit, last: Commit): Int {
        val output = CommandRunner.runLocally(
            project,
            commitDistanceCommand.withParam("${first.hash}..${last.hash}")
        )
        val distance = output.stdout.trim().toIntOrNull() ?: 0
        return distance - 1
    }

    /**
     * Base command for [remoteBranchExist] and [localBranchExist]
     */
    private const val branchExistCommand = "git rev-parse --verify"

    /**
     * Returns true if a branch named [branchName] exists on the development server.
     */
    fun remoteBranchExist(project: Project, branchName: String): Boolean {
        return CommandRunner.runRemotely(project, branchExistCommand.withParam(branchName)).exitCode == 0
    }

    /**
     * Returns true if a branch named [branchName] exists on the local.
     */
    fun localBranchExist(project: Project, branchName: String): Boolean {
        return CommandRunner.runLocally(project, branchExistCommand.withParam(branchName)).exitCode == 0
    }

    /**
     * Base command for [localLastCommit]
     */
    private const val lastCommitCommand = "git rev-parse HEAD"

    /**
     * Returns the latest commit on the development server.
     */
    fun localLastCommit(project: Project): Commit {
        val output = CommandRunner.runLocally(project, lastCommitCommand)
        val hash = output.stdout.trim()
        if (hash.isEmpty()) {
            LOG.warn("Empty local commit hash, command output stderr: ${output.stderr}")
        }

        val command = listOf(
            "git", "log", "-1",
            hash,
            "--format=${Commit.OUTPUT_FORMAT}".replace("\"", ""),
        )
        return Commit.fromString(CommandRunner.runLocally(project, command).stdout)
    }

    /**
     * Base command for [remoteCommitsInRange] and [localCommitsInRange]
     */
    private const val commitRangeCommand = "git log --format=${Commit.OUTPUT_FORMAT}"

    /**
     * Returns commits from the development server between [startCommit] and [endCommit] inclusive.
     */
    fun remoteCommitsInRange(project: Project, startCommit: Commit, endCommit: Commit): Pair<List<Commit>, Int> {
        val countCommits = remoteCountCommitsInRange(project, startCommit, endCommit)

        val output = CommandRunner.runRemotely(
            project,
            commitRangeCommand
                .withParam("-30")
                .withParam("${startCommit.hash}..${endCommit.hash}")
        )
        return Pair(outputToCommits(output), countCommits)
    }

    /**
     * Returns commits count from the development server between [startCommit] and [endCommit] inclusive.
     */
    fun remoteCountCommitsInRange(project: Project, startCommit: Commit, endCommit: Commit): Int {
        return CommandRunner.runRemotely(
            project,
            countCommitsCommand
                .withParam("${startCommit.hash}..${endCommit.hash}")
        ).stdout.trim().toIntOrNull() ?: -1
    }

    /**
     * Returns commits from the local between [startCommit] and [endCommit] inclusive.
     */
    fun localCommitsInRange(project: Project, startCommit: Commit, endCommit: Commit): Pair<List<Commit>, Int> {
        val countCommits = localCountCommitsInRange(project, startCommit, endCommit)

        val output = CommandRunner.runLocally(
            project,
            commitRangeCommand
                .withParam("-30")
                .withParam("${startCommit.hash}..${endCommit.hash}")
                .replace("\"", "")
        )
        return Pair(outputToCommits(output), countCommits)
    }

    /**
     * Returns commits count from the local between [startCommit] and [endCommit] inclusive.
     */
    fun localCountCommitsInRange(project: Project, startCommit: Commit, endCommit: Commit): Int {
        return CommandRunner.runLocally(
            project,
            countCommitsCommand
                .withParam("${startCommit.hash}..${endCommit.hash}")
        ).stdout.trim().toIntOrNull() ?: -1
    }

    /**
     * Helper function for parsing the output into the commit list.
     */
    private fun outputToCommits(output: Output): List<Commit> {
        if (output.stdout.isBlank()) return emptyList()

        val rawCommits = output.stdout.trim()
            .removeSuffix(Commit.COMMIT_SEPARATOR)
            .split(Commit.COMMIT_SEPARATOR)
        return rawCommits.map {
            Commit.fromString(it)
        }
    }

    /**
     * Base command for [remoteStash]
     */
    private const val stashCommand = "git stash save -u"

    /**
     * Creates a new `git stash` on the development server with the following name:
     *
     *   `AdmStorm automatic synchronization (dd.M.yyyy hh:mm:ss)`
     */
    fun remoteStash(project: Project): Output {
        return CommandRunner.runRemotely(project, stashCommand.withParam(stashMessage()))
    }

    /**
     * Creates a new `git stash` on the local with the following name:
     *
     *   `AdmStorm generated stash (dd.M.yyyy hh:mm:ss)`
     */
    fun localStash(project: Project): Output {
        return CommandRunner.runLocally(project, stashCommand.withParam(stashMessage()))
    }

    private fun stashMessage(): String {
        val sdf = SimpleDateFormat("dd.M.yyyy hh:mm:ss")
        val currentDate = sdf.format(Date())
        return "\"AdmStorm generated stash ($currentDate)\""
    }

    /**
     * Base command for [remoteStashAndDrop]
     */
    private const val stashAndDropCommand = "$stashCommand 'drop stash' && git stash drop"

    /**
     * Creates a temporary stash on the development server and immediately removes it.
     *
     * This can be useful if you want to remove all current changes in the repository.
     */
    fun remoteStashAndDrop(project: Project): Output {
        return CommandRunner.runRemotely(project, stashAndDropCommand)
    }

    /**
     * Base command for [countNewServerCommits], [countNewLocalCommits],
     * [remoteCountCommitsInRange] and [localCountCommitsInRange]
     */
    private const val countCommitsCommand = "git rev-list --count"

    /**
     * Returns the number of commits on the development server that
     * are not yet pushed to Gitlab.
     */
    fun countNewServerCommits(project: Project, branch: String): Int {
        return CommandRunner.runLocally(
            project,
            countCommitsCommand
                .withParam("origin/$branch..")
        ).stdout.trim().toIntOrNull() ?: -1
    }

    /**
     * Returns the number of commits on the local that
     * are not yet pushed to the development server.
     */
    fun countNewLocalCommits(project: Project, branchName: String): Int {
        return CommandRunner.runLocally(
            project,
            countCommitsCommand
                .withParam("${ServerNameProvider.name()}/$branchName..")
        ).stdout.trim().toIntOrNull() ?: -1
    }

    /**
     * Base command for [newServerCommits]
     */
    private const val newServerCommitsCommand = "git log --format=${Commit.OUTPUT_FORMAT}"

    /**
     * Returns a list of commits on the development server that
     * are not yet pushed to Gitlab.
     */
    fun newServerCommits(project: Project, branchName: String): List<Commit> {
        val output = CommandRunner.runRemotely(
            project,
            newServerCommitsCommand
                .withParam("origin/$branchName..")
        )

        return outputToCommits(output)
    }

    /**
     * Base command for [remoteCommitsSinceMaster]
     */
    private const val commitsSinceMasterCommand = "git log --format=${Commit.OUTPUT_FORMAT} master..HEAD"

    /**
     * Returns a list of commits that have been created since master.
     */
    fun remoteCommitsSinceMaster(project: Project): List<Commit> {
        val output = CommandRunner.runRemotely(
            project,
            commitsSinceMasterCommand
        )

        return outputToCommits(output)
    }

    /**
     * Base command for [remoteHardReset]
     */
    private const val hardResetCommand = "git reset --hard"

    /**
     * Launches hard reset on the development server.
     */
    fun remoteHardReset(project: Project): Output {
        return CommandRunner.runRemotely(project, hardResetCommand)
    }

    /**
     * Launches hard reset on the local.
     */
    fun localHardReset(project: Project): Output {
        return CommandRunner.runLocally(project, hardResetCommand)
    }

    /**
     * Base command for [remoteAddFileToIndex] and [localAddFileToIndex]
     */
    private const val addFileToIndexCommand = "git add"

    /**
     * Adds a file named [filename] to the index in the local repository.
     */
    fun localAddFileToIndex(project: Project, filename: String): Output {
        return CommandRunner.runLocally(
            project,
            listOf("git", "add", filename)
        )
    }

    /**
     * Adds a file named [filename] to the index on the development server.
     */
    fun remoteAddFileToIndex(project: Project, filename: String): Output {
        return CommandRunner.runRemotely(
            project,
            addFileToIndexCommand.withParam(filename)
        )
    }

    /**
     * Base command for [localCommitFiles] and [remoteCommitFiles]
     */
    private const val commitFilesCommand = "git diff-tree --no-commit-id --name-status -r"

    /**
     * Returns the local files modified in this commit.
     */
    fun localCommitFiles(project: Project, commit: Commit): List<String> {
        val stdout = CommandRunner.runLocally(
            project,
            commitFilesCommand.withParam(commit.hash.asString())
        ).stdout.trim()

        return if (stdout.isEmpty()) emptyList() else stdout.split("\n")
    }

    /**
     * Returns the files from development server modified in this commit.
     */
    fun remoteCommitFiles(project: Project, commit: Commit): List<String> {
        val stdout = CommandRunner.runRemotely(
            project,
            commitFilesCommand.withParam(commit.hash.asString())
        ).stdout.trim()

        return if (stdout.isEmpty()) emptyList() else stdout.split("\n")
    }


    /**
     * Base command for [localParentCommit] and [remoteParentCommit]
     */
    private const val parentCommitCommand = "git log --pretty=%P -n 1"

    /**
     * Returns local parent commit for passed commit.
     */
    fun localParentCommit(project: Project, commit: Commit): Commit {
        val parentHash = CommandRunner.runLocally(
            project,
            parentCommitCommand.withParam(commit.hash.asString())
        ).stdout.trim()

        val commitDataCommand = "git log -1 --format=${Commit.OUTPUT_FORMAT}"
        val parentCommitData = CommandRunner.runLocally(
            project,
            commitDataCommand.replace("\"", "").withParam(parentHash)
        ).stdout

        return Commit.fromString(parentCommitData)
    }

    /**
     * Returns the parent commit for passed commit from development server.
     */
    fun remoteParentCommit(project: Project, commit: Commit): Commit {
        val parentHash = CommandRunner.runRemotely(
            project,
            parentCommitCommand.withParam(commit.hash.asString())
        ).stdout.trim()

        val commitDataCommand = "git log -1 --format=${Commit.OUTPUT_FORMAT}"
        val parentCommitData = CommandRunner.runRemotely(
            project,
            commitDataCommand.withParam(parentHash)
        ).stdout

        return Commit.fromString(parentCommitData)
    }


    /**
     * Base command for [remoteGetPermission]
     */
    private const val getPermissionCommand = "stat -c \"%a\""

    /**
     * Returns the permissions for the passed file on the development server.
     */
    fun remoteGetPermission(project: Project, filename: String): String? {
        return CommandRunner.runRemotely(
            project,
            getPermissionCommand.withParam(filename)
        ).stdout.trim().ifEmpty { null }
    }

    /**
     * Base command for [remoteGetPermission]
     */
    private const val setPermissionCommand = "chmod"

    /**
     * Sets the permissions for the passed file on the development server.
     */
    fun remoteSetPermission(project: Project, permissions: String, filename: String): Output {
        return CommandRunner.runRemotely(
            project,
            setPermissionCommand
                .withParam(permissions)
                .withParam(filename)
        )
    }

    fun remoteStashAndAction(project: Project, onReady: Runnable) {
        runBackground(project, "Stash ${ServerNameProvider.name()} changes") {
            val output = remoteStash(project)
            if (output.exitCode != 0) {
                MessageDialog.showError(
                    """
                        Unable to execute ${GitUIUtil.code("git stash")}
                        
                        ${output.stderr}
                    """.trimIndent(),
                    "Problem with Stash on ${ServerNameProvider.name()}"
                )
                return@runBackground
            }

            onReady.run()
        }
    }

    fun localStashAndAction(project: Project, onReady: Runnable) {
        runBackground(project, "Stash local changes") {
            val output = localStash(project)
            if (output.exitCode != 0) {
                MessageDialog.showError(
                    """
                        Unable to execute ${GitUIUtil.code("git stash")}
                        
                        ${output.stderr}
                    """.trimIndent(),
                    "Problem with Stash on local"
                )
                return@runBackground
            }

            onReady.run()
        }
    }

    fun remoteDropFiles(project: Project) {
        LOG.info("Run 'git reset --hard' on ${ServerNameProvider.name()}")
        val output = remoteHardReset(project)
        if (output.exitCode != 0) {
            MessageDialog.showWarning(
                """
                    Unable to execute ${GitUIUtil.code("git reset --hard")}
                    
                    ${output.stderr}
                """.trimIndent(),
                "Problem with hard reset on ${ServerNameProvider.name()}"
            )
            LOG.warn("Unable to execute 'git reset --hard' on ${ServerNameProvider.name()}: ${output.stderr}")
        }
    }

    fun localDropFiles(project: Project) {
        LOG.info("Run 'git reset --hard' on local")
        val output = localHardReset(project)
        if (output.exitCode != 0) {
            MessageDialog.showWarning(
                """
                    Unable to execute ${GitUIUtil.code("git reset --hard")}
                    
                    ${output.stderr}
                """.trimIndent(),
                "Problem with hard reset on local"
            )
            LOG.warn("Unable to execute 'git reset --hard' on local: ${output.stderr}")
        }
    }

    /**
     * Base command for [currentUser]
     */
    private const val getUserNameCommand = "git -c core.quotepath=false config --null --get user.name"
    private const val getUserEmailCommand = "git -c core.quotepath=false config --null --get user.email"

    data class User(val name: String?, val email: String?)

    /**
     * Returns the current git user.
     *
     * @return if a local git [User] isn't set, returns the global git [User], if it isn't set returns null.
     */
    fun currentUser(project: Project): User? {
        val localUser = getUser(project, local = true)
        if (localUser != null) {
            return localUser
        }

        val globalUser = getUser(project, local = false)
        if (globalUser != null) {
            return globalUser
        }

        return null
    }

    private fun getUser(project: Project, local: Boolean): User? {
        val flag = if (local) "--local" else ""
        val outputName = CommandRunner.runLocally(project, getUserNameCommand.withParam(flag)).stdout
        if (outputName.isEmpty()) {
            return null
        }

        val outputEmail = CommandRunner.runLocally(project, getUserEmailCommand.withParam(flag)).stdout

        return User(outputName, outputEmail)
    }

    /**
     * Helper function for building commands.
     *
     * Function recognizes when the passed parameter has spaces and
     * is not a string and escapes them.
     *
     * @param param part to be attached to the command
     * @param needSpace if true, a space will be inserted before [param]
     */
    fun String.withParam(param: String, needSpace: Boolean = true): String {
        val space = if (needSpace) " " else ""

        if (param.startsWith("\"") || param.startsWith("'")) {
            return this + space + param
        }

        return this + space + param.replace(" ", "\\ ")
    }
}
