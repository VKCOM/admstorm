package com.vk.admstorm.git.sync.commits

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.changes.Change
import com.intellij.util.containers.MultiMap
import com.intellij.vcs.log.Hash
import com.intellij.vcs.log.VcsCommitMetadata
import com.intellij.vcs.log.VcsLogObjectsFactory
import com.intellij.vcs.log.impl.HashImpl
import com.intellij.vcs.log.impl.VcsUserImpl
import com.intellij.vcs.log.ui.frame.CommitPresentationUtil
import com.intellij.vcsUtil.VcsUtil
import com.vk.admstorm.actions.git.panels.changes.GitLazyContentRevision
import com.vk.admstorm.actions.git.panels.changes.GitLocalContentRevision
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MyUtils
import git4idea.GitRevisionNumber
import java.util.*

data class Commit(
    val hash: Hash = HashImpl.build("bdcc9cef8129d35c5c5588164fcaa78e5611e2df"),

    val author: String = "",
    val authorDate: Long = 0,
    val authorEmail: String = "",

    val committer: String = "",
    val committerDate: Long = 0,
    val committerEmail: String = "",

    val subject: String = "",
    val body: String = "",

    var fromRemote: Boolean = false,
) {
    fun asMetaData(project: Project): VcsCommitMetadata {
        val root = MyUtils.virtualFileByRelativePath(project, ".git")
        return project.service<VcsLogObjectsFactory>().createCommitMetadata(
            hash,
            emptyList(), committerDate, root, subject, author,
            authorEmail, subject, committer, committerEmail, authorDate,
        )
    }

    fun presentation(project: Project): CommitPresentationUtil.CommitPresentation {
        val root = MyUtils.virtualFileByRelativePath(project, ".git")!!
        val hashAndAuthor = CommitPresentationUtil.formatCommitHashAndAuthor(
            hash, VcsUserImpl(author, authorEmail), authorDate,
            VcsUserImpl(committer, committerEmail),
            committerDate
        )
        return CommitPresentationUtil.CommitPresentation(
            project, root, subject, hashAndAuthor, MultiMap.empty()
        )
    }

    private fun fileStatusByString(str: String): FileStatus {
        return when (str) {
            "D" -> FileStatus.DELETED
            "M" -> FileStatus.MODIFIED
            "A" -> FileStatus.ADDED
            "I" -> FileStatus.IGNORED
            "X" -> FileStatus.UNKNOWN
            else -> FileStatus.MODIFIED
        }
    }

    /**
     * Returns a list of changes in the current commit.
     *
     * First, we get the list of changed files in this commit.
     * Next, we construct revisions, where the current revision is created from the current
     * content of the file (see [GitLocalContentRevision]), and the previous one from the
     * content of the previous commit (see [GitLazyContentRevision]).
     *
     * If the commit came from the development server, then for now we return an empty list.
     */
    fun getChanges(project: Project): List<Change> {
        // TODO: improve this
        if (fromRemote) return emptyList()

        val changedFiles = GitUtils.localCommitFiles(project, this)
        if (changedFiles.isEmpty()) return emptyList()

        val parentCommit = GitUtils.localParentCommit(project, this)

        val beforeRev = GitRevisionNumber(parentCommit.hash.asString(), Date(parentCommit.committerDate))
        val afterRev = GitRevisionNumber(hash.asString(), Date(committerDate))

        return changedFiles.map {
            val parts = it.split("\t")
            val status = fileStatusByString(parts[0].trim())
            val path = parts[1].trim()

            val fullPath = MyPathUtils.absoluteLocalPath(project, path)

            val localFile = MyUtils.virtualFileByRelativePath(project, path)
            val afterContent =
                if (localFile == null) ""
                else if (localFile.fileType.isBinary) "Binary"
                else LoadTextUtil.loadText(localFile).toString()

            val before = GitLazyContentRevision(project, VcsUtil.getFilePath(fullPath, false), beforeRev)
            val after = GitLocalContentRevision(afterContent, VcsUtil.getFilePath(fullPath, false), afterRev)

            when (status) {
                FileStatus.ADDED -> Change(null, after)
                FileStatus.DELETED -> Change(before, null)
                else -> Change(before, after, status)
            }
        }
    }

    companion object {
        const val COMMIT_SEPARATOR = "<<<--->>>"
        private const val SUBJECT_BODY_SEPARATOR = "<<!>>"

        // Hash
        // Author
        // Author Date Unix
        // Author Email
        // Committer
        // Committer Date Unix
        // Committer Email
        // Subject
        // Body
        const val OUTPUT_FORMAT =
            "\"%H;%an;%at;%aE;%cn;%ct;%cE$SUBJECT_BODY_SEPARATOR%s$SUBJECT_BODY_SEPARATOR%b$COMMIT_SEPARATOR\""

        fun fromString(output: String): Commit {
            val parts = output
                .trim()
                .removeSuffix(COMMIT_SEPARATOR)
                .split(SUBJECT_BODY_SEPARATOR)
            if (parts.size != 3) {
                return Commit()
            }

            val mainPart = parts[0].split(";")
            if (mainPart.size != 7) {
                return Commit()
            }

            val subject = parts[1]
            val body = parts[2]

            return Commit(
                HashImpl.build(mainPart[0]),
                mainPart[1],
                (mainPart[2].toLongOrNull() ?: 0) * 1000,
                mainPart[3],
                mainPart[4],
                (mainPart[5].toLongOrNull() ?: 0) * 1000,
                mainPart[6],
                subject,
                body,
            )
        }
    }
}
