package com.vk.admstorm.utils

import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.remote.ColoredRemoteProcessHandler
import com.intellij.ssh.ExecBuilder
import com.intellij.ssh.channels.SftpChannel
import com.intellij.ssh.process.SshExecProcess
import com.intellij.util.ReflectionUtil
import com.vk.admstorm.CommandRunner.runLocally
import com.vk.admstorm.env.Env
import com.vk.admstorm.notifications.AdmErrorNotification
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.ssh.SshHandler
import com.vk.admstorm.ssh.YubikeyHandler
import com.vk.admstorm.utils.MyUtils.executeOnPooledThread
import git4idea.util.GitUIUtil.code
import net.schmizz.sshj.connection.channel.OpenFailException
import net.schmizz.sshj.sftp.SFTPClient
import java.lang.reflect.Field
import java.nio.charset.Charset
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object MySshUtils {
    private val LOG = logger<MySshUtils>()

    /**
     * @returns [SFTPClient] for the passed [sftpChannel] through reflection hack.
     */
    fun getSftpClient(sftpChannel: SftpChannel): SFTPClient? {
        try {
            @Suppress("INVISIBLE_REFERENCE")
            val sftpChannelClass =
                com.intellij.ssh.impl.sshj.channels.SshjSftpChannel::class.java
            val field =
                ReflectionUtil.findFieldInHierarchy(sftpChannelClass) { f: Field ->
                    f.type == SFTPClient::class.java
                } ?: throw IllegalArgumentException("Unable to upload files!")
            field.isAccessible = true
            return field[sftpChannel] as SFTPClient
        } catch (e: Exception) {
            AdmErrorNotification(
                "Failed to get sftp client for this session, please try it again: ${e.message}",
            ).show()
        }

        return null
    }

    fun openTunnel(project: Project, port: Int, processListener: ProcessListener) {
        val credentials = SshConnectionService.getInstance(project).credentials() ?: return

        val command = "ssh -vCNR $port:localhost:$port -N ${credentials.userName}@${credentials.host}"

        executeOnPooledThread {
            runLocally(project, command, true, processListener)
        }
    }

    fun exec(
        project: Project,
        command: String,
        firstLine: String = command,
        workingDir: String? = null
    ): ColoredRemoteProcessHandler<SshExecProcess>? {
        val startTime = System.currentTimeMillis()
        val workingDirectory = workingDir ?: Env.data.projectRoot

        LOG.info("Start SSH command execution (command: $command, workingDir: $workingDirectory)")

        val builder = SshConnectionService.getInstance(project).sshExecBuilder("cd $workingDirectory && $command")
        if (builder == null) {
            LOG.warn(
                """
                    SshConnectionService.sshExecBuilder() == null, 
                    SshConnectionService.isConnected() == ${SshConnectionService.getInstance(project).isConnected()}
                """.trimIndent()
            )
            return null
        }

        val process = try {
            SshHandler.handle {
                execSync(builder)
            }
        } catch (ex: OpenFailException)  {
            handleSshException(project, ex)
            null
        } catch (ex: TimeoutException) {
            AdmWarningNotification("Don't forget to touch the yubikey if it blinks when using the AdmStorm plugin's features")
                .withTitle("Yubikey waiting timeout")
                .show()
            LOG.info("Yubikey waiting timeout", ex)
            null
        } catch (ex: IllegalStateException) {
            handleSshException(project, ex)
            null
        } catch (ex: Exception) {
            val exceptionName = ex.javaClass.name
            LOG.error("Unhandled exception", ex)
            AdmErrorNotification("Unhandled exception $exceptionName").show()
            null
        } ?: return null

        val elapsedTime = System.currentTimeMillis() - startTime
        LOG.info("Elapsed time to start SSH command: ${elapsedTime}ms")

        return ColoredRemoteProcessHandler(process, firstLine, Charset.defaultCharset())
    }

    private fun handleSshException(project: Project, ex: Exception) {
        val message = "${ex.message}<br>" +
                "Plugin can try to automatically reset the Yubikey or you can do it yourself with " +
                code("ssh-agent")

        AdmWarningNotification(message)
            .withTitle("SSH connection lost")
            .withActions(
                AdmNotification.Action("Reset Yubikey and Reconnect...") { _, notification ->
                    notification.expire()

                    val success = YubikeyHandler().autoReset(project) {
                        SshConnectionService.getInstance(project).connect()
                    }

                    if (!success) {
                        return@Action
                    }
                    SshConnectionService.getInstance(project).connect()
                }
            )
            .withActions(
                AdmNotification.Action("Reconnect...") { _, notification ->
                    notification.expire()
                    SshConnectionService.getInstance(project).connect()
                }
            )
            .show()
        LOG.warn("Unexpected exception for execSync(builder)", ex)
    }

    private fun execSync(builder: ExecBuilder): SshExecProcess {
        // Start execution in a separate thread in separate cases
        // when the call builder.execute() gives an exception.
        if (ApplicationManager.getApplication().isDispatchThread ||
            ApplicationManager.getApplication().isWriteThread ||
            ApplicationManager.getApplication().isReadAccessAllowed
        ) {
            val processFuture = ApplicationManager.getApplication()
                .executeOnPooledThread(Callable {
                    try {
                        Pair(builder.execute(), null)
                    } catch (e: Exception) {
                        LOG.warn("Exception while run 'builder.execute()'", e)
                        Pair(null, e)
                    }
                })

            val res = processFuture.get(10, TimeUnit.SECONDS)
            if (res == null) {
                LOG.warn("'processFuture.get == null'")
                throw IllegalStateException("processFuture.get is null")
            }

            // was an exception
            if (res.second != null) {
                LOG.warn("Re-throw exception from 'builder.execute()'", res.second!!)
                throw res.second!!
            }

            if (res.first == null) {
                LOG.warn("builder.execute() is null'")
                throw IllegalStateException("builder.execute() is null")
            }

            return res.first!!
        }

        return builder.execute()
    }
}
