package com.vk.admstorm.ssh

import com.intellij.execution.Output
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.vk.admstorm.env.Env
import git4idea.util.GitUIUtil.bold

object LostConnectionHandler {
    private val LOG = Logger.getInstance(LostConnectionHandler::class.java)
    private const val lostConnectionMessage = "Permission denied"

    fun handle(project: Project, output: Output, action: Runnable): Boolean {
        if (!output.stderr.contains(lostConnectionMessage)) return false

        LOG.warn("Lost SSH connection for local commands: ${output.stderr}")

        ApplicationManager.getApplication().invokeAndWait {
            val dialog = MessageDialogBuilder.yesNo(
                "Not Connected to ${Env.data.serverName}",
                """
                    Oops, it seems impossible to execute the internal command due to SSH connection problems.

                    Try reconnecting and push ${bold("Try Again")} button.
                """.trimIndent()
            )
                .asWarning()
                .noText("Cancel")
                .yesText("Try Again")

            if (dialog.ask(project)) {
                LOG.warn("Trying to reconnect")
                action.run()
            } else {
                LOG.warn("Dialog closed without trying to reconnect")
            }
        }

        return true
    }
}
