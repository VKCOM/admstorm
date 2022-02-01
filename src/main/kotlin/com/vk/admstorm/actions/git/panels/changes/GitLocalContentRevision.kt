package com.vk.admstorm.actions.git.panels.changes

import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.changes.ByteBackedContentRevision
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.openapi.vfs.encoding.EncodingRegistry
import git4idea.GitRevisionNumber
import java.nio.charset.Charset

/**
 * Revision for a locally accessible file.
 *
 * Considers the file encoding when returning content as a byte array.
 */
class GitLocalContentRevision(
    private var myContent: String,
    private val myFilePath: FilePath,
    private val myRevNumber: GitRevisionNumber,
) : ContentRevision, ByteBackedContentRevision {

    override fun getContent() = myContent
    override fun getFile() = myFilePath
    override fun getRevisionNumber() = myRevNumber

    override fun getContentAsBytes(): ByteArray {
        val encoding = EncodingRegistry.getInstance().getEncoding(file.virtualFile, true)
            ?: Charset.forName("windows-1251")
        return content.toByteArray(encoding)
    }
}
