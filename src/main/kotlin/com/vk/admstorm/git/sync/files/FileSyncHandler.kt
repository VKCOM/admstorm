package com.vk.admstorm.git.sync.files

import com.intellij.execution.Output
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.env.Env
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.transfer.TransferService
import com.vk.admstorm.ui.MessageDialog
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MyUtils.runBackground
import git4idea.util.GitUIUtil
import java.io.File
import java.nio.charset.Charset

class FileSyncHandler(
    private val myProject: Project
) {
    companion object {
        private val LOG = Logger.getInstance(FileSyncHandler::class.java)
    }

    private fun removeRemoteFile(project: Project, path: String): Output {
        return CommandRunner.runRemotely(project, "rm $path")
    }

    private fun renameRemoteFileWithGit(project: Project, old: String, new: String): Output {
        return CommandRunner.runRemotely(project, "git mv $old $new")
    }

    private fun renameLocalFileWithGit(project: Project, old: String, new: String): Output {
        return CommandRunner.runLocally(project, "git mv $old $new")
    }

    fun removeFileOnServer(remoteFile: RemoteFile, onReady: Runnable? = null) {
        runBackground(myProject, "Remove ${remoteFile.path} on ${Env.data.serverName}") {
            val output = removeRemoteFile(myProject, remoteFile.path)
            if (output.exitCode != 0) {
                MessageDialog.showWarning(
                    """
                        Can't remove ${GitUIUtil.code(remoteFile.path)}:
                        
                        ${output.stderr}
                        """.trimIndent(),
                    "Problem with removing file on ${Env.data.serverName}"
                )
                return@runBackground
            }

            onReady?.run()
        }
    }

    fun createLocalFileFromRemote(remoteFile: RemoteFile, onReady: Runnable? = null) {
        runBackground(
            myProject,
            "Create new local file ${remoteFile.path} from ${Env.data.serverName} content"
        ) {
            val filepath = MyPathUtils.absoluteLocalPath(myProject, remoteFile.path)

            val newFile = File(filepath)
            try {
                newFile.parentFile.mkdirs()
                newFile.createNewFile()
            } catch (e: Exception) {
                MessageDialog.showWarning(
                    """
                        Can't create new file ${GitUIUtil.code(remoteFile.path)}:
                        
                        ${e.message}
                    """.trimIndent(),
                    "Problem with creating new file on ${Env.data.serverName}"
                )
                return@runBackground
            }
            val charset = try {
                Charset.forName(remoteFile.encoding)
            } catch (e: Exception) {
                Charset.forName("windows-1251")
            }

            newFile.writeText(remoteFile.content, charset)

            val output = GitUtils.localAddFileToIndex(myProject, filepath)
            if (output.exitCode != 0) {
                MessageDialog.showWarning(
                    """
                        Can't add file ${GitUIUtil.code(remoteFile.path)} to git:
                        
                        ${output.stderr}
                    """.trimIndent(),
                    "Problem with adding new file on ${Env.data.serverName}"
                )
                return@runBackground
            }

            onReady?.run()
        }
    }

    fun rewriteLocalFileWithRemoteContent(localFile: VirtualFile, remoteContent: String, onReady: Runnable) {
        ApplicationManager.getApplication().runWriteAction {
            FileDocumentManager.getInstance().getDocument(localFile)?.setText(remoteContent)
            val doc = FileDocumentManager.getInstance().getDocument(localFile)
            if (doc != null) {
                FileDocumentManager.getInstance().saveDocument(doc)
                onReady.run()
            }
        }
    }

    fun rewriteRemoteFileWithLocalContent(localFile: VirtualFile, onReady: Runnable? = null) {
        ApplicationManager.getApplication().invokeLater {
            val remotePath = MyPathUtils.remotePathByLocalPath(myProject, localFile.path)
            val perm = GitUtils.remoteGetPermission(myProject, remotePath)
            LOG.info("File '$remotePath' permissions is $perm")

            TransferService.getInstance(myProject).uploadFile(localFile) {
                // after upload
                val output = GitUtils.remoteAddFileToIndex(myProject, remotePath)
                if (output.exitCode != 0) {
                    MessageDialog.showWarning(
                        """
                        Can't add file ${GitUIUtil.code(remotePath)} to git on ${Env.data.serverName}:
                        
                        ${output.stderr}
                        """.trimIndent(),
                        "Problem with git add"
                    )
                    return@uploadFile
                }

                if (perm != null) {
                    val setPermOutput = GitUtils.remoteSetPermission(myProject, perm, remotePath)
                    if (setPermOutput.exitCode != 0) {
                        MessageDialog.showWarning(
                            """
                            Can't set permission ${GitUIUtil.code(perm)} for ${GitUIUtil.code(remotePath)}:
                            
                            ${output.stderr}
                            """.trimIndent(),
                            "Problems with setting permission"
                        )
                        return@uploadFile
                    }
                }

                onReady?.run()
            }
        }
    }

    fun renameRemoteFile(remoteFile: RemoteFile, onReady: Runnable? = null) {
        if (remoteFile.origPath == null) return

        ProgressManager.getInstance().run(object : Task.ConditionalModal(
            myProject,
            "Rename ${remoteFile.path} to ${remoteFile.origPath} on ${Env.data.serverName}",
            true,
            PerformInBackgroundOption.DEAF,
        ) {
            override fun run(indicator: ProgressIndicator) {
                val renameOutput = renameRemoteFileWithGit(project, remoteFile.origPath, remoteFile.path)
                if (renameOutput.exitCode != 0) {
                    MessageDialog.showWarning(
                        """
                            Can't rename ${GitUIUtil.code(remoteFile.origPath)} to ${GitUIUtil.code(remoteFile.path)}:
                            
                            ${renameOutput.stderr}
                        """.trimIndent(),
                        "Problems with file rename"
                    )
                }

                if (remoteFile.localFile.virtualFile != null) {
                    rewriteRemoteFileWithLocalContent(remoteFile.localFile.virtualFile, onReady)
                }
            }
        })
    }

    fun renameLocalFile(remoteFile: RemoteFile, onReady: Runnable? = null) {
        if (remoteFile.localFile.virtualFile == null || remoteFile.origPath == null) {
            return
        }

        ProgressManager.getInstance().run(object : Task.ConditionalModal(
            myProject,
            "Rename ${remoteFile.origPath} to ${remoteFile.path} locally",
            true,
            PerformInBackgroundOption.DEAF,
        ) {
            override fun run(indicator: ProgressIndicator) {
                val output = renameLocalFileWithGit(project, remoteFile.path, remoteFile.origPath)
                if (output.exitCode != 0) {
                    MessageDialog.showWarning(
                        """
                            Can't rename ${GitUIUtil.code(remoteFile.origPath)} to ${GitUIUtil.code(remoteFile.path)}:
                            
                            ${output.stderr}
                        """.trimIndent(),
                        "Problems with file rename"
                    )
                    return
                }

                onReady?.run()
            }
        })
    }
}
