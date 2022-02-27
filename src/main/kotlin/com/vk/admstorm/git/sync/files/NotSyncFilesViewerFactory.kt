package com.vk.admstorm.git.sync.files

import com.intellij.openapi.fileTypes.ex.FileTypeChooser
import com.intellij.openapi.project.Project
import com.vk.admstorm.utils.ServerNameProvider

class NotSyncFilesViewerFactory(
    project: Project,
    remoteFile: RemoteFile,
    localFilePath: String,
    localFileContent: String?,
) {
    val viewer: FilesContentViewer

    init {
        val fileType = FileTypeChooser.getKnownFileTypeOrAssociate(remoteFile.path)
        val remoteContent = if (remoteFile.isNotFound) null else remoteFile.content

        val firstContent =
            if (remoteContent == null)
                null
            else
                FilesContentViewer.Content(
                    ServerNameProvider.name(),
                    "${ServerNameProvider.name()} content",
                    remoteFile.path,
                    remoteContent,
                )

        val secondContent =
            if (localFileContent == null)
                null
            else
                FilesContentViewer.Content(
                    "Local",
                    "Local content",
                    localFilePath,
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
