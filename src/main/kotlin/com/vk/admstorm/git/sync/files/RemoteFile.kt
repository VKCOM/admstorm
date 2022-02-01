package com.vk.admstorm.git.sync.files

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.vk.admstorm.git.sync.SyncChecker

data class RemoteFile(
    val path: String,
    val origPath: String?,
    val content: String,
    val encoding: String,
    val md5: String,
    val isNotFound: Boolean,
    val isRemoved: Boolean,
    val isRenamed: Boolean,

    val localFile: LocalFile,
) {
    companion object {
        val LOG = Logger.getInstance(RemoteFile::class.java)

        /**
         * data format:
         *   $filename<separator>$encoding<separator>$md5<separator>$content
         *
         * if file not found content must be equal "<not-found>" or "<renamed>"
         * if file was removed (in git status) content must be equal "<removed>"
         */
        fun create(project: Project, data: String): RemoteFile? {
            val parts = data.split(SyncChecker.PartSeparator)
            if (parts.size != 5) {
                LOG.warn("Incorrect file format: $data")
                return null
            }

            val (filename, origPath, encoding, md5, content) = parts
            val isNotFound = content == "<not-found>"
            val isRemoved = content == "<removed>"
            val isRenamed = content == "<renamed>"

            return RemoteFile(
                filename,
                origPath.ifEmpty { null },
                content,
                encoding,
                md5,
                isNotFound,
                isRemoved,
                isRenamed,
                localFile = LocalFile.create(project, filename),
            )
        }
    }

    fun equalWithLocal(): Boolean {
        return md5 == localFile.md5 &&
                isRemoved == localFile.isRemoved
    }
}
