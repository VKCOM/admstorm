package com.vk.admstorm.ssh

import com.intellij.execution.Output
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageConstants
import com.intellij.openapi.ui.MessageDialogBuilder
import com.vk.admstorm.utils.ServerNameProvider
import git4idea.util.GitUIUtil.code

object LostConnectionHandler {
    private val LOG = Logger.getInstance(LostConnectionHandler::class.java)
    private const val LOST_CONNECTION_MESSAGE = "Permission denied"

    fun handle(project: Project, output: Output, action: Runnable): Boolean {
        if (!output.stderr.contains(LOST_CONNECTION_MESSAGE)) return false

        LOG.warn("Lost SSH connection for local commands: ${output.stderr}")

        ApplicationManager.getApplication().invokeAndWait {
            val dialog = MessageDialogBuilder.yesNoCancel(
                "Not Connected to ${ServerNameProvider.name()}",
                """
                    Oops, it seems impossible to execute the internal command due to SSH connection problems.

                    Plugin can try to automatically reset the Yubikey and run command again or you can do it 
                    yourself with ${code("ssh-agent")} and push 'Try Again' button.
                """.trimIndent()
            )
                .asWarning()
                .yesText("Auto Reset and Try Again")
                .noText("Try Again")
                .cancelText("Cancel")

            when (dialog.show(project)) {
                MessageConstants.YES -> {
                    LOG.info("Trying to auto-reset Yubikey in LostConnectionHandler")
                    YubikeyHandler().autoReset(project) {
                        LOG.info("Trying to start again in autoReset ofFail in LostConnectionHandler")
                        action.run()
                        return@autoReset
                    }

                    action.run()
                }
                MessageConstants.NO -> {
                    LOG.info("Trying to reconnect in LostConnectionHandler")
                    action.run()
                }
                else -> {
                    LOG.info("Dialog closed without trying to reconnect in LostConnectionHandler")
                }
            }
        }

        return true
    }
}
