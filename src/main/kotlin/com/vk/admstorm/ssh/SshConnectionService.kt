package com.vk.admstorm.ssh

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.remote.RemoteConnector
import com.intellij.remote.RemoteCredentials
import com.intellij.ssh.ConnectionBuilder
import com.intellij.ssh.ExecBuilder
import com.intellij.ssh.channels.SftpChannel
import com.intellij.ssh.connectionBuilder
import com.jetbrains.plugins.remotesdk.console.SshConfigConnector
import com.vk.admstorm.AdmStartupService
import com.vk.admstorm.notifications.AdmErrorNotification
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.transfer.TransferService
import com.vk.admstorm.utils.MySshUtils
import com.vk.admstorm.utils.MyUtils.executeOnPooledThread
import git4idea.util.GitUIUtil.code
import net.schmizz.sshj.connection.channel.OpenFailException
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.TransportException
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Service responsible for connecting via SSH to the development server.
 */
@Service(Service.Level.PROJECT)
class SshConnectionService(private var myProject: Project) : Disposable {
    companion object {
        fun getInstance(project: Project) = project.service<SshConnectionService>()

        private val LOG = logger<SshConnectionService>()
    }

    private var myConnector: SshConfigConnector? = null
    private var myCredentials: RemoteCredentials? = null
    private var myConnectionBuilder: ConnectionBuilder? = null

    private var mySftpChannel: SftpChannel? = null
    private var mySftpClient: SFTPClient? = null

    /**
     * See [TransferService.downloadFile] for usage example.
     */
    fun sftpChannel() = mySftpChannel

    /**
     * See [TransferService.transfer] for usage example.
     */
    fun sftpClient() = mySftpClient

    fun credentials() = myCredentials

    /**
     * Returns the builder for the passed command.
     *
     * See [MySshUtils.exec] for usage example.
     */
    fun sshExecBuilder(command: String): ExecBuilder? {
        return myConnectionBuilder?.execBuilder(command)
    }

    /**
     * Checks if it is connected via SSH to the server.
     *
     * @return true if connected via SSH
     */
    fun isConnected(): Boolean {
        return mySftpChannel != null && mySftpChannel!!.isConnected
    }

    /**
     * Checks if it is connected via SSH to the server and if not, then a
     * notification is shown with an action for a quick connection.
     *
     * @return true if connected via SSH
     */
    fun isConnectedOrWarning(): Boolean {
        if (isConnected()) return true

        AdmWarningNotification("To perform this command, connect via SSH to the required server")
            .withTitle("No connection with server")
            .withActions(
                AdmNotification.Action("Connect...") { e, notification ->
                    notification.expire()
                    connect {
                        AdmStartupService.getInstance(e.project!!).afterConnectionTasks()
                    }
                }
            )
            .show()

        LOG.warn("Using commands without SSH connection")
        return false
    }

    /**
     * Establishes an SSH connection using the passed SSH configuration.
     */
    private fun connectWithConnector(connector: RemoteConnector, onSuccessful: Runnable? = null) {
        if (connector !is SshConfigConnector) {
            LOG.info("Connector is not SshConfigConnector")
            return
        }

        myConnector = connector

        connector.produceRemoteCredentials { myCredentials = it }
        if (myCredentials == null) {
            LOG.warn("Credentials is null")
            return
        }

        // Disconnect the current connection, if it exists.
        disconnect()

        executeOnPooledThread {
            // See also [com.jetbrains.plugins.remotesdk.tools.RemoteTool.startRemoteProcess]
            @Suppress("UnstableApiUsage")
            myConnectionBuilder = myCredentials!!.connectionBuilder(
                myProject,
                ProgressManager.getGlobalProgressIndicator(),
                // `allowDialogs` is a flag that allows dialogs.
                // For example, if at the moment it is not possible to connect to
                // the server, then if this flag is true (by default), then the
                // IDE will show a window in which you will be prompted to enter
                // a password for connection.
                allowDialogs = false
            ).withConnectionTimeout(10, TimeUnit.SECONDS)

            ProgressManager.getInstance().run(object : Task.Backgroundable(
                myProject,
                "AdmStorm: Connecting to ${getSshConnectionName()}",
                true
            ) {
                private var cancelled = false

                override fun run(indicator: ProgressIndicator) {
                    try {
                        SshHandler.handle {
                            mySftpChannel = myConnectionBuilder!!.openSftpChannel(2)
                            mySftpClient = MySshUtils.getSftpClient(mySftpChannel!!)

                            onSuccessful?.run()
                        }
                    } catch (ex: OpenFailException) {
                        val message =
                            "${ex.message}<br> Plugin can try to automatically reset the Yubikey or you can do it yourself with ${
                                code("ssh-agent")
                            }"

                        AdmErrorNotification(message, true)
                            .withTitle("Failed to connect to server")
                            .withActions(AdmNotification.Action("Reset Yubikey and Connect...") { _, notification ->
                                notification.expire()

                                val success = YubikeyHandler().autoReset(project) {
                                    connectWithConnector(connector, onSuccessful)
                                }

                                if (!success) {
                                    return@Action
                                }
                                connectWithConnector(connector, onSuccessful)
                            })
                            .withActions(AdmNotification.Action("Connect...") { _, notification ->
                                notification.expire()
                                connectWithConnector(connector, onSuccessful)
                            })
                            .show()

                        LOG.warn("Failed to connect", ex)
                    } catch (ex: TimeoutException) {
                        if (indicator.isCanceled) {
                            LOG.info("Cancelled by user", ex)
                            return
                        }

                        AdmNotification("Touch the yubikey when it blinks while using AdmStorm")
                            .withTitle("Yubikey waiting timeout")
                            .withActions(AdmNotification.Action("Reconnect...") { _, notification ->
                                notification.expire()
                                connectWithConnector(connector, onSuccessful)
                            }).show()

                        LOG.info("Yubikey waiting timeout", ex)
                    } catch (ex: TransportException) {
                        if (ex.message == null) {
                            LOG.error("Transport exception: ${ex.javaClass.name}")
                            return
                        }

                        val message =
                            "${ex.javaClass.name}: ${ex.message}"
                        if (ex.message!!.contains("HostKeyAlgorithms")) {
                            AdmErrorNotification(message, true)
                                .withTitle("Did you switch your SSH config type to LEGACY?")
                                .withActions(AdmNotification.Action("Reconnect...") { _, notification ->
                                    notification.expire()
                                    connectWithConnector(connector, onSuccessful)
                                }).show()
                            LOG.info("Not correct ssh config type", ex)
                            return
                        }

                        AdmErrorNotification(message, true)
                            .withTitle("Did you connect to corporate access system?")
                            .withActions(AdmNotification.Action("Reconnect...") { _, notification ->
                                notification.expire()
                                connectWithConnector(connector, onSuccessful)
                            }).show()
                        LOG.info("Corporate access error", ex)
                    } catch (ex: Exception) {
                        val errorMessage = ex.javaClass.name
                        LOG.error("Unhandled exception $errorMessage")
                        AdmErrorNotification("Unhandled exception $errorMessage").show()
                        return
                    }
                }

                override fun onCancel() {
                    super.onCancel()
                    cancelled = true
                }
            })
        }
    }

    /**
     * Shows a list of available configurations and establishes an SSH connection
     * using the user-selected SSH configuration.
     * If no configuration is available, SSH configuration options are opened.
     *
     * After successful connection, calls the [onSuccessful] function.
     */
    fun connect(onSuccessful: Runnable? = null) {
        try {
            RemoteDataProducerWrapper()
                .withProject(myProject)
                .produceRemoteDataWithConnector(null, null, myProject) { connector ->
                    connectWithConnector(connector, onSuccessful)
                }
        } catch (e: Exception) {
            AdmErrorNotification(
                e.message ?: "Failed to create a connection, please try it later",
            ).show()
            LOG.warn("Failed to create a connection", e)
        }
    }

    /**
     * Establishes an SSH connection using the first available SSH configuration.
     * If the configuration is not available, then, unlike [connect], nothing happens.
     *
     * After successful connection, calls the [onSuccessful] function.
     */
    fun tryConnectSilence(onSuccessful: Runnable? = null) {
        try {
            RemoteDataProducerWrapper()
                .withProject(myProject)
                .produceRemoteDataWithFirstConnector(null, null, myProject) { connector ->
                    connectWithConnector(connector, onSuccessful)
                }
        } catch (e: Exception) {
            AdmErrorNotification(
                e.message ?: "Failed to create a connection, please try it later",
            ).show()
            LOG.warn("Failed to create a connection", e)
        }
    }

    /**
     * Disconnects the current SSH connection if it exists.
     */
    fun disconnect() {
        LOG.info("Disconnecting from SSH")
        if (mySftpChannel != null && mySftpChannel!!.isConnected) {
            try {
                mySftpChannel!!.close()
            } catch (e: IOException) {
                LOG.warn("Unexpected exception while disconnect", e)
            }
        }
        myConnectionBuilder = null
        mySftpChannel = null
        mySftpClient = null
    }

    /**
     * Returns a string representation of the connection.
     *
     * Example:
     *
     *     pmakhnev@server:8080
     */
    fun getSshConnectionName() = when {
        myConnector != null -> myConnector!!.name
        myCredentials != null -> "${myCredentials!!.userName}@${myCredentials!!.host}:${myCredentials!!.port}"
        else -> ""
    }

    override fun dispose() {
        disconnect()
    }
}
