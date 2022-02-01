package com.vk.admstorm.git.sync.files

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.EncodingRegistry
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MyUtils
import git4idea.branch.GitBranchUtil
import git4idea.index.GitFileStatus
import git4idea.index.LightFileStatus
import git4idea.index.NUL
import git4idea.index.isRenamed
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object GitStatus {
    // See https://github.com/Kotlin/kotlinx.serialization/issues/993
    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    @Serializable
    data class ExtendedFile(
        val path: String,
        val origPath: String?,
        val encoding: String,
        val md5sum: String,
        val isRenamed: Boolean,
        val isConflicted: Boolean,
        val isUntracked: Boolean,
        val isIgnored: Boolean,
        val isRemoved: Boolean,
    )

    private val LOG = Logger.getInstance(GitStatus::class.java)

    fun GitFileStatus.isDeleted() = workTree == 'D' || index == 'D'

    /**
     * Returns a json representation of the passed files.
     */
    fun asJson(files: List<ExtendedFile>) = Json.encodeToString(files)

    /**
     * Returns a list of files from local git status not including untracked.
     */
    fun localStatus(project: Project): List<ExtendedFile> {
        val startTime = System.currentTimeMillis()

        val repo = GitBranchUtil.getCurrentRepository(project)!!
        val root = repo.root

        var files = Parser.rawLocalStatus(project, root, includeUntracked = false)

        if (files.size > 100) {
            files = files.subList(0, 100)
            AdmWarningNotification(
                "You need to manually reduce this number in order for the plugin to work correctly!"
            )
                .withTitle("Too many files in git status")
                .show()

            LOG.warn("Too many files in git status (${files.size})")
        }

        val extendedFiles = files.mapNotNull { file ->
            val relPath = MyPathUtils.relativeLocalPath(project, file.path.path)

            // most likely the file was deleted
            if (file.path.virtualFile == null) {
                return@mapNotNull ExtendedFile(
                    relPath, origPath = null, isRenamed = false,
                    isConflicted = false, isUntracked = false, isIgnored = false,
                    isRemoved = true, md5sum = "", encoding = ""
                )
            }

            if (file.path.virtualFile!!.isDirectory) {
                return@mapNotNull null
            }

            val encoding = EncodingRegistry.getInstance().getEncoding(file.path.virtualFile, true)
            val encodingName = encoding?.name() ?: "windows-1251"

            ExtendedFile(
                relPath,
                file.origPath?.path?.let {
                    MyPathUtils.relativeLocalPath(project, it)
                },
                encodingName,
                MyUtils.md5file(file.path.virtualFile),
                isRenamed = file.origPath != null,
                file.isConflicted(),
                file.isUntracked(),
                file.isIgnored(),
                false,
            )
        }

        val elapsedTime = System.currentTimeMillis() - startTime
        LOG.info("Elapsed time to get local file status with md5: ${elapsedTime}ms")

        return extendedFiles
    }

    /**
     * Returns a list of files from the remote git status.
     *
     * @param includeUntracked if true, then untracked files are ignored
     */
    fun rawRemoteStatus(project: Project, root: VirtualFile, includeUntracked: Boolean = false): List<GitFileStatus> {
        return Parser.rawRemoteStatus(project, root, includeUntracked)
    }

    internal object Parser {
        fun rawRemoteStatus(
            project: Project,
            root: VirtualFile,
            includeUntracked: Boolean = false
        ): List<GitFileStatus> {
            return getFileStatus(project, false, includeUntracked)
                .map { GitFileStatus(root, it) }
        }

        fun rawLocalStatus(
            project: Project,
            root: VirtualFile,
            includeUntracked: Boolean = false
        ): List<GitFileStatus> {
            return getFileStatus(project, true, includeUntracked)
                .map { GitFileStatus(root, it) }
        }

        private fun getFileStatus(
            project: Project,
            local: Boolean = true,
            includeUntracked: Boolean = false
        ): List<LightFileStatus.StatusRecord> {
            val startTime = System.currentTimeMillis()

            var command = "git status --porcelain -z"
            if (!includeUntracked) command += " -uno"

            val output =
                if (local) CommandRunner.runLocally(project, command, trimOutput = false)
                else CommandRunner.runRemotely(project, command)

            LOG.info("Git status output: '${output.stdout}'")

            if (output.exitCode != 0) {
                return emptyList()
            }

            val res = parseGitStatusOutput(output.stdout)

            LOG.info("Parse git status result: '${res.joinToString(",")}'")

            val elapsedTime = System.currentTimeMillis() - startTime
            LOG.info("Elapsed time to get ${if (local) "local" else "remote"} file status: $elapsedTime" + "ms")

            return res
        }

        private fun parseGitStatusOutput(output: String): List<LightFileStatus.StatusRecord> {
            if (output.isEmpty()) {
                return emptyList()
            }

            val result = mutableListOf<LightFileStatus.StatusRecord>()

            val split = output.split(NUL).toTypedArray()
            val it = split.iterator()
            while (it.hasNext()) {
                val line = it.next()
                if (StringUtil.isEmptyOrSpaces(line)) continue // skip empty lines if any (e.g. the whole output may be empty on a clean working tree).
                if (line.startsWith("starting fsmonitor-daemon in ")) continue // skip debug output from experimental daemon in git-for-windows-2.33
                // format: XY_filename where _ stands for space.
                if (line.length < 4 || line[2] != ' ') { // X, Y, space and at least one symbol for the file
                    return emptyList()
                }

                val xStatus = line[0]
                val yStatus = line[1]
                if (!isKnownStatus(xStatus) || !isKnownStatus(yStatus)) {
                    return emptyList()
                }

                val pathPart = line.substring(3) // skipping the space
                if (isRenamed(xStatus) || isRenamed(yStatus)) {
                    if (!it.hasNext()) {
                        continue
                    }
                    val origPath = it.next() // read the "from" filepath which is separated also by NUL character.
                    result.add(LightFileStatus.StatusRecord(xStatus, yStatus, pathPart, origPath = origPath))
                } else {
                    result.add(LightFileStatus.StatusRecord(xStatus, yStatus, pathPart))
                }
            }

            return result
        }

        private fun isKnownStatus(status: Char): Boolean {
            return status == ' ' || status == 'M' || status == 'A' ||
                    status == 'D' || status == 'C' || status == 'R' ||
                    status == 'U' || status == 'T' || status == '!' ||
                    status == '?'
        }
    }
}
