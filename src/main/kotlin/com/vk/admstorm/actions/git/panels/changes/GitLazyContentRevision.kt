package com.vk.admstorm.actions.git.panels.changes

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.changes.ByteBackedContentRevision
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.encoding.EncodingRegistry
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.git.GitUtils.withParam
import com.vk.admstorm.utils.MyPathUtils
import git4idea.GitRevisionNumber
import java.io.File
import java.nio.charset.Charset

/**
 * Revision for a file to be fetched from git from a specific commit.
 * See [getContentAsBytes] for details.
 *
 * @param myRevNumber hash of the commit from which the contents of the file will be taken.
 */
class GitLazyContentRevision(
    private val myProject: Project,
    private val myFilePath: FilePath,
    private val myRevNumber: GitRevisionNumber,
) : ContentRevision, ByteBackedContentRevision {

    companion object {
        private val LOG = Logger.getInstance(GitLazyContentRevision::class.java)
    }

    private var myContent = ""

    override fun getContent() = contentAsBytes.contentToString()
    override fun getFile(): FilePath = myFilePath
    override fun getRevisionNumber(): VcsRevisionNumber = myRevNumber

    override fun getContentAsBytes(): ByteArray {
        val encoding = EncodingRegistry.getInstance().getEncoding(file.virtualFile, true)
            ?: Charset.forName("windows-1251")

        if (myContent.isEmpty()) {
            val fileContentCommand = "git show"
            val path = MyPathUtils.absoluteLocalPath(myProject, "file_content.txt")
            val tempFile = File(path)

            val relFilepath = MyPathUtils.relativeLocalPath(myProject, myFilePath.path)
            CommandRunner.runLocallyToFile(
                myProject,
                fileContentCommand.withParam("${myRevNumber.rev}:$relFilepath"),
                tempFile
            )

            myContent = tempFile.readText(encoding)

            try {
                tempFile.delete()
            } catch (e: SecurityException) {
                LOG.warn("Unable to delete file ${file.path}", e)
            }
        }

        return myContent.toByteArray(encoding)
    }
}
