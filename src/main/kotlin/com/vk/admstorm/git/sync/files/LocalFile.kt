package com.vk.admstorm.git.sync.files

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.EncodingRegistry
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MyUtils
import java.io.File

data class LocalFile(
    val path: String,
    val content: String,
    val encoding: String,
    val md5: String,
    val isRemoved: Boolean,
    val isNotFound: Boolean,
    val virtualFile: VirtualFile?,
) {
    companion object {
        fun create(project: Project, name: String): LocalFile {
            val absPath = if (File(name).isAbsolute) {
                name
            } else {
                MyPathUtils.absoluteLocalPath(project, name)
            }

            val virtualFile = ApplicationManager.getApplication().runReadAction(Computable {
                MyUtils.virtualFileByName(absPath)
            })

            val isRemoved = virtualFile == null

            val encoding = EncodingRegistry.getInstance().getEncoding(virtualFile, true)

            val content = if (virtualFile != null && !virtualFile.fileType.isBinary)
                ApplicationManager.getApplication().runReadAction(Computable {
                    LoadTextUtil.loadText(virtualFile).toString()
                })
            else ""

            return LocalFile(
                name,
                content,
                encoding?.displayName() ?: "windows-1251",
                MyUtils.md5file(virtualFile),
                isRemoved,
                isRemoved,
                virtualFile
            )
        }
    }
}
