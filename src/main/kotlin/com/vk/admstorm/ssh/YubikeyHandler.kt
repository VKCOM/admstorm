package com.vk.admstorm.ssh

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.execution.Output
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.settings.AdmStormSettingsState
import com.vk.admstorm.ui.MessageDialog
import git4idea.util.GitUIUtil
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class YubikeyHandler {
    companion object {
        private val LOG = logger<YubikeyHandler>()
    }

    fun autoReset(project: Project, onFail: Runnable): Boolean {
        LOG.info("Try auto reset Yubikey")
        val resetScript = createScriptIfNotExists(project) ?: return false

        val credentialAttributes =
            CredentialAttributes(generateServiceName("admstorm", "yubikey-pass"))

        val needAskPassword = AdmStormSettingsState.getInstance().askYubikeyPassword ||
                PasswordSafe.instance.getPassword(credentialAttributes) == null

        var needSavePassword = false
        val password = if (needAskPassword) {
            LOG.info("Requesting a password from the user")
            EnterPasswordDialog.requestPassword(project) {
                AdmStormSettingsState.getInstance().askYubikeyPassword = false
                needSavePassword = true
            }
        } else {
            LOG.info("Retrieving the password from the password vault")
            PasswordSafe.instance.getPassword(credentialAttributes)!!
        }

        val killOutput = CommandRunner.runLocally(project, "pkill ssh-agen")
        if (killOutput.exitCode != 0) {
            LOG.warn("pkill ssh-agen exited with non-zero code while Yubikey reset")
        }

        val evalOutput = CommandRunner.runLocallyEval("ssh-agent -s")
        if (evalOutput == null) {
            LOG.warn("Unexpected null output while eval 'ssh-agent -s' for Yubikey reset")
            showYubikeyResetFailNotification(project, "ssh-agent -s exited with non-zero code", null, onFail)
            return false
        }
        if (evalOutput.exitCode != 0) {
            LOG.warn("eval 'ssh-agent -s' exited with non-zero code while Yubikey reset")
        }

        LOG.info("Started the main command to reset")

        val echoBuilder = ProcessBuilder("echo", password)
        val sshAddBuilder = ProcessBuilder("ssh-add", "-s", "/usr/local/lib/opensc-pkcs11.so")

        sshAddBuilder.environment().apply {
            put("SSH_ASKPASS_REQUIRE", "force")
            put("SSH_ASKPASS", resetScript.absolutePath)
        }

        val processes = try {
            ProcessBuilder.startPipeline(
                listOf(
                    echoBuilder,
                    sshAddBuilder
                )
            )
        } catch (e: IOException) {
            LOG.warn("Unexpected exception while startPipeline for Yubikey reset", e)
            showYubikeyResetFailNotification(project, "Unable to run reset commands", null, onFail)
            return false
        }

        processes.forEach {
            it.waitFor(30, TimeUnit.SECONDS)
        }

        val proc = processes.last()

        val out = Output(
            proc.inputStream.bufferedReader().readText(),
            proc.errorStream.bufferedReader().readText(),
            proc.exitValue()
        )
        val result = if (!out.stderr.contains("Card added")) {
            showYubikeyResetFailNotification(project, "Possibly an incorrect password was entered", out, onFail)
            needSavePassword = false
            false
        } else true

        if (needSavePassword) {
            LOG.info("Saving the new password")
            PasswordSafe.instance.setPassword(credentialAttributes, password)
        } else {
            LOG.info("Removing the old password")
            PasswordSafe.instance.setPassword(credentialAttributes, null)
        }

        return result
    }

    private fun createScriptIfNotExists(project: Project): File? {
        val home = System.getProperty("user.home")
        val resetScript = File("$home/.admstorm-yubikey-reset-script.sh")
        if (!resetScript.exists()) {
            LOG.info("Script '.admstorm-yubikey-reset-script.sh' not found, creating a new one")

            try {
                resetScript.createNewFile()
                resetScript.writeText(
                    """
                        #!/bin/bash
                        # AdmStorm plugin script
                        # 
                        # This script is automatically created if it is not found 
                        # when auto reset is selected in the plugin.
                        # 
                        # This little script is needed to redirect the password via 
                        # stdout to utilities that support SSH_ASKPASS.
                        # For example: ssh-add
                        read SECRET
                        echo ${'$'}SECRET
                    """.trimIndent()
                )

                val output = CommandRunner.runLocally(project, "chmod +x ${resetScript.absolutePath}")
                if (output.exitCode != 0) {
                    MessageDialog.showWarning(
                        """
                        Can't set executable permission for script '${GitUIUtil.code(resetScript.absolutePath)}' for reset Yubikey.
                        Try to do it manually, without this auto reset is not possible.
                        
                        stderr:
                        ${output.stderr}
                    """.trimIndent(),
                        "Problem with setting permission"
                    )
                    return null
                }
            } catch (e: Exception) {
                MessageDialog.showWarning(
                    """
                        Can't create script '${GitUIUtil.code(resetScript.absolutePath)}' for reset Yubikey:
                        
                        ${e.message}
                    """.trimIndent(),
                    "Problem with creating Yubikey reset script"
                )
                return null
            }
        }

        return resetScript
    }

    private fun showYubikeyResetFailNotification(project: Project, reason: String, out: Output?, onFail: Runnable) {
        AdmWarningNotification(
            """
                $reason.
                Try again or reset Yubikey via terminal and click on 'Just Reconnect'.
            """.trimIndent()
        )
            .withActions(AdmNotification.Action("Try Again...") { _, notification ->
                notification.expire()

                val success = autoReset(project, onFail)
                if (!success) {
                    return@Action
                }

                onFail.run()
            })
            .withActions(AdmNotification.Action("Just Reconnect") { _, notification ->
                notification.expire()
                onFail.run()
            })
            .withTitle("Automatic Yubikey reset failed")
            .show()

        LOG.warn(
            """
                Automatic Yubikey reset failed: $reason
                
                stdout: ${out?.stdout}
                stderr: ${out?.stderr}
                exit code: ${out?.exitCode}
            """.trimIndent()
        )
    }
}
