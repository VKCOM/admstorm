package com.vk.admstorm.ui

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.messages.MessagesService
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object MessageDialog {
    private val LOG = logger<MessageDialog>()

    fun showError(message: String, title: String, project: Project? = null) {
        val icon = IconLoader.getIcon("general/errorDialog.svg", this::class.java)
        show(message, title, icon, project)
    }

    fun showWarning(message: String, title: String, project: Project? = null) {
        val icon = IconLoader.getIcon("general/warningDialog.svg", this::class.java)
        show(message, title, icon, project)
    }

    fun showSuccess(message: String, title: String, project: Project? = null) {
        val icon = IconLoader.getIcon("general/successDialog.svg", this::class.java)
        show(message, title, icon, project)
    }

    private fun show(message: String, title: String, icon: Icon?, project: Project? = null) {
        invokeLater {
            LOG.info("MessageDialog: title: $title, content: $message")
            MessagesService.getInstance().showMessageDialog(
                project = project, parentComponent = null,
                message = message, title = title, icon = icon,
                options = arrayOf("Ok"),
                doNotAskOption = null,
                helpId = null, alwaysUseIdeaUI = true
            )
        }
    }
}
