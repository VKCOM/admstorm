package com.vk.admstorm.actions.git

import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.fileTypes.ex.FileTypeChooser
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FileStatus
import com.vk.admstorm.env.Env
import com.vk.admstorm.git.sync.files.FilesContentViewer
import com.vk.admstorm.utils.MyUtils
import git4idea.index.GitFileStatus

class GitStatusFilesViewerFactory(
    project: Project,
    localFile: GitFileStatus,
    remoteFilePath: String,
    remoteFileContent: String,
) {
    val viewer: FilesContentViewer

    init {
        val isModified = localFile.getStagedStatus() == FileStatus.MODIFIED ||
                localFile.getUnStagedStatus() == FileStatus.MODIFIED

        val localFileContent = if (isModified) {
            val filepath = localFile.path.path
            val localVirtualFile = MyUtils.virtualFileByName(filepath)
            val localFileContent =
                if (localVirtualFile == null) null
                else LoadTextUtil.loadText(localVirtualFile).toString()

            localFileContent
        } else {
            null
        }

        val fileType = FileTypeChooser.getKnownFileTypeOrAssociate(localFile.path.path)

        val firstContent =
            if (remoteFileContent.isEmpty())
                null
            else
                FilesContentViewer.Content(
                    Env.data.serverName,
                    "${Env.data.serverName} content",
                    remoteFilePath,
                    remoteFileContent,
                )

        val secondContent =
            if (localFileContent == null)
                null
            else
                FilesContentViewer.Content(
                    "Local",
                    "Local content",
                    localFile.path.path,
                    localFileContent
                )

        viewer = FilesContentViewer(
            project,
            fileType,
            firstContent,
            secondContent
        )
    }
}
