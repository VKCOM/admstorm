package com.vk.admstorm.transfer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ssh.RemoteFileObject
import com.vk.admstorm.AdmStormStartupActivity
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.notifications.AdmErrorNotification
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.ServerNameProvider
import net.schmizz.sshj.common.StreamCopier
import net.schmizz.sshj.sftp.SFTPFileTransfer
import net.schmizz.sshj.xfer.TransferListener
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.net.SocketException
import java.util.function.Consumer
import kotlin.math.roundToInt

/**
 * Service responsible for downloading and uploading files to the development server.
 */
@Service
class TransferService(private var myProject: Project) {
    companion object {
        fun getInstance(project: Project) = project.service<TransferService>()

        private val LOG = Logger.getInstance(TransferService::class.java)
        private const val EDITABLE_FILE_SIZE = (2 * 1024 * 1024).toLong()
    }

    private val mySshService = SshConnectionService.getInstance(myProject)
    private val myRemoteEditingFiles = HashMap<RemoteFileObject, String>(100)

    /**
     * Launches uploading the passed [localFile] to the development server.
     *
     * The path to the file on the development server will be equivalent to
     * the current path relative to the project root.
     *
     * If the upload fails, a notification will be displayed.
     *
     * After the file is uploaded to the server, the [onReady] callback function will be called.
     */
    fun uploadFile(localFile: VirtualFile, remotePath: String = localFile.path, onReady: Runnable? = null) {
        val startTime = System.currentTimeMillis()

        val sftpChannel = mySshService.sftpChannel()
        if (sftpChannel == null || !sftpChannel.isConnected) {
            noConnectionWarning(upload = true)
            return
        }

        val remoteAbsPath = MyPathUtils.remotePathByLocalPath(myProject, remotePath)
        val remotePathFolder = File(remoteAbsPath).parent

        // Since the file may be located in a folder that is still not on the server,
        // it is necessary first to create the entire folder hierarchy, otherwise
        // the upload will fail.
        val commandResult = CommandRunner.runRemotely(
            myProject,
            "mkdir -p $remotePathFolder"
        )
        if (commandResult.exitCode != 0) {
            AdmWarningNotification("Unable to create folders '$remotePathFolder' to upload file")
                .withTitle("AdmStorm internal error")
                .show()
            LOG.warn("Unable to create folders '$remotePathFolder' to upload file")
            return
        }

        val remoteFile = sftpChannel.file(remoteAbsPath)
        val localIOFile = File(localFile.path)

        ApplicationManager.getApplication().executeOnPooledThread {
            transfer(localIOFile, remoteFile, TransferType.UPLOAD) { res ->
                when (res.result) {
                    TransferResult.SUCCESS -> {
                        onReady?.run()
                    }
                    TransferResult.FAIL -> {
                        val actions = if (res.exception == "No SFTP session available!") {
                            arrayOf(
                                AdmNotification.Action("Reconnect...") { _, notification ->
                                    notification.expire()
                                    mySshService.connect()
                                }
                            )
                        } else emptyArray()

                        AdmWarningNotification(res.exception ?: "")
                            .withTitle("Transfer failed")
                            .withActions(*actions)
                            .show()

                        LOG.warn("Transfer of ${localFile.path} failed: $res", Exception(res.exception))
                    }
                    TransferResult.CANCELLED -> {
                        AdmWarningNotification(res.exception ?: "")
                            .withTitle("Transfer canceled")
                            .show()

                        LOG.warn("Transfer of ${localFile.path} canceled: $res", Exception(res.exception))
                    }
                    else -> {
                        LOG.warn("Transfer of ${localFile.path}: $res", Exception(res.exception))
                    }
                }

                val elapsed = System.currentTimeMillis() - startTime
                LOG.info("Elapsed time to upload file ${localFile.path} to ${ServerNameProvider.name()}: ${elapsed}ms")
            }
        }
    }

    /**
     * Launches download the file with passed [filepath] from the development server.
     *
     * The file path must match the path on the server.
     *
     * If the download fails, a notification will be displayed.
     *
     * After the file is downloaded from the server, the [onReady] callback function will be called.
     * The first argument is a temp file to which the contents were written.
     */
    fun downloadFile(filepath: String, onReady: Consumer<File>? = null) {
        val sftpChannel = mySshService.sftpChannel()
        if (sftpChannel == null || !sftpChannel.isConnected) {
            noConnectionWarning(upload = false)
            return
        }

        val remoteFile = sftpChannel.file(filepath)
        if (remoteFile.isDir()) {
            LOG.warn("Can not edit a folder '${remoteFile.path()}'")
            return
        }

        invokeLater {
            if (remoteFile.size() > EDITABLE_FILE_SIZE) {
                val needDownload = MessageDialogBuilder.yesNo(
                    "This file is too large for text editor",
                    "Do you still want to download and edit it?"
                )
                    .asWarning()
                    .ask(myProject)

                if (!needDownload) {
                    return@invokeLater
                }
            }

            myRemoteEditingFiles
                .keys.stream()
                .filter { rf: RemoteFileObject -> rf.path() == remoteFile.path() }
                .findFirst()
                .orElse(null)?.let { existsRemoteFile ->
                    myRemoteEditingFiles[existsRemoteFile]?.let { localFile ->
                        val oldCachedFile = File(localFile)
                        if (oldCachedFile.exists()) {
                            myRemoteEditingFiles.remove(existsRemoteFile)
                        }
                    }
                }

            try {
                val remoteFileName = remoteFile.name()
                val localFile = File.createTempFile("adm-", remoteFileName)

                ApplicationManager.getApplication().executeOnPooledThread {
                    transfer(localFile, remoteFile, TransferType.DOWNLOAD) { result ->
                        if (result.result == TransferResult.SUCCESS) {
                            myRemoteEditingFiles[remoteFile] = localFile.absolutePath
                            onReady?.accept(localFile)
                        }
                    }
                }
            } catch (e: IOException) {
                AdmErrorNotification("Unable to create cache file: ${e.message}").show()
                LOG.warn("Unexpected download exception", e)
            }
        }
    }

    /**
     * General function for uploading and downloading files.
     *
     * @param localFile file that will be uploaded to the server or a file
     *                  in which the content of a file from the server will be placed
     * @param remoteFile file that will be downloaded from the server or a file
     *                   in which the content of the loaded local file will be placed
     *
     *                   To get a RemoteFileObject instance call
     *                      myAdmService.sftpChannel().file()
     *                   with the file path
     * @param type [TransferType.DOWNLOAD] or [TransferType.UPLOAD]
     * @param onTransferResult function that is called at the end of the download or an error
     */
    @Synchronized
    private fun transfer(
        localFile: File,
        remoteFile: RemoteFileObject,
        type: TransferType,
        onTransferResult: Consumer<TransferFileModel>? = null,
    ) {
        val sftpChannel = mySshService.sftpChannel()
        val sftpClient = mySshService.sftpClient()
        if (sftpChannel == null || !sftpChannel.isConnected) {
            onTransferResult?.accept(
                TransferFileModel(
                    type = type, source = "", target = "", size = 0,
                    result = TransferResult.FAIL, exception = "No SFTP session available!"
                )
            )
            return
        }

        val normalizedRemotePath = mySshService.getSshConnectionName() + ":" + normalizeRemoteFileObjectPath(remoteFile)
        val remoteFilePath = remoteFile.path()
        val localFileAbsPath = localFile.absolutePath

        val transferFileModel = TransferFileModel(
            type = type,
            source = if (type === TransferType.UPLOAD) localFileAbsPath else normalizedRemotePath,
            target = if (type === TransferType.UPLOAD) normalizedRemotePath else localFileAbsPath,
            size = if (type === TransferType.UPLOAD) localFile.length() else remoteFile.size(),
        )

        try {
            if (type === TransferType.UPLOAD) {
                if (!localFile.exists()) {
                    throw TransferException("Can't find local file $localFileAbsPath")
                }
            } else {
                if (!remoteFile.exists()) {
                    throw TransferException("Can't find remote file $remoteFilePath")
                }
            }

            // run in EDT to start the download while the dialog is open
            invokeLater(ModalityState.any()) {
                ProgressManager.getInstance().run(object : Task.Backgroundable(
                    myProject,
                    "Admstorm: " + if (type === TransferType.UPLOAD) "Uploading" else "Downloading",
                ) {
                    override fun run(indicator: ProgressIndicator) {
                        indicator.isIndeterminate =
                            if (type === TransferType.UPLOAD)
                                localFile.isDirectory
                            else
                                remoteFile.isDir()

                        try {
                            val fileTransfer = SFTPFileTransfer(sftpClient!!.sftpEngine)

                            fileTransfer.transferListener = object : TransferListener {
                                override fun directory(name: String): TransferListener {
                                    return this
                                }

                                override fun file(name: String, size: Long): StreamCopier.Listener {
                                    val total = FileUtils.byteCountToDisplaySize(size)
                                    return StreamCopier.Listener { transferred: Long ->
                                        if (indicator.isCanceled || transferFileModel.result === TransferResult.CANCELLED) {
                                            throw TransferCancelledException("Operation cancelled!")
                                        }
                                        transferFileModel.transferred = transferred
                                        indicator.text2 =
                                            "${(if (type === TransferType.UPLOAD) "Uploading" else "Downloading")} " +
                                                    "${transferFileModel.source} to ${transferFileModel.target}"
                                        val percent = transferred.toDouble() / size

                                        if (!indicator.isIndeterminate) {
                                            indicator.fraction = percent
                                        }
                                        indicator.text = "${((percent * 10000).roundToInt() / 100)}% " +
                                                "${FileUtils.byteCountToDisplaySize(transferred)}/$total"
                                    }
                                }
                            }

                            if (type === TransferType.UPLOAD) {
                                fileTransfer.upload(localFileAbsPath, remoteFilePath)
                            } else {
                                fileTransfer.download(remoteFilePath, localFileAbsPath)
                            }

                            transferFileModel.result = TransferResult.SUCCESS
                            onTransferResult?.accept(transferFileModel)
                        } catch (e: TransferCancelledException) {
                            transferFileModel.result = TransferResult.CANCELLED
                            transferFileModel.exception = e.message ?: "cancelled"
                            onTransferResult?.accept(transferFileModel)
                        } catch (e: Exception) {
                            transferFileModel.result = TransferResult.FAIL
                            transferFileModel.exception = e.message ?: "failed"
                            onTransferResult?.accept(transferFileModel)

                            AdmWarningNotification(
                                "Error occurred while transferring ${transferFileModel.source} to ${transferFileModel.target}, ${e.message}",
                            ).show()
                            if (e is SocketException) {
                                mySshService.disconnect()
                            }
                        }
                    }

                    override fun onCancel() {
                        super.onCancel()
                        this.cancelText = "Cancelling..."
                    }
                })
            }
        } catch (e: Exception) {
            transferFileModel.result = TransferResult.FAIL
            transferFileModel.exception = e.message ?: "transfer failed"
            onTransferResult?.accept(transferFileModel)

            LOG.warn("Transfer file exception (localFile: ${localFile.name}, remoteFile: ${remoteFile.path()}) ", e)
        }
    }

    private fun noConnectionWarning(upload: Boolean) {
        AdmWarningNotification(
            """
                Unable to ${if (upload) "upload" else "download"} file because there is no SSH connection. 
                <br>
                Try reconnecting to the server.
            """.trimIndent()
        )
            .withActions(
                AdmNotification.Action("Connect...") { e, notification ->
                    notification.expire()
                    SshConnectionService.getInstance(e.project!!).connect {
                        AdmStormStartupActivity.getInstance(e.project!!).afterConnectionTasks(e.project!!)
                    }
                }
            )
            .show()
    }

    private fun normalizeRemoteFileObjectPath(file: RemoteFileObject): String {
        return if (file.path().startsWith("//")) file.path().substring(1) else file.path()
    }
}
