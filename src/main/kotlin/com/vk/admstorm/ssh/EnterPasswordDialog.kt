package com.vk.admstorm.ssh

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.JPanel

class EnterPasswordDialog(project: Project) : DialogWrapper(project, true, IdeModalityType.PROJECT) {
    companion object {
        fun requestPassword(project: Project, ifRemember: () -> Unit = {}): String {
            val dialog = EnterPasswordDialog(project)
            dialog.showAndGet()
            if (dialog.isRemember()) {
                ifRemember()
            }

            return dialog.getPassword()
        }
    }

    private lateinit var myPanel: JPanel
    private lateinit var myPasswordInput: JBPasswordField
    private val myRememberCheckBox = JBCheckBox("Remember")

    init {
        title = "Enter Password"

        init()
    }

    fun getPassword() = String(myPasswordInput.password)
    fun isRemember() = myRememberCheckBox.isSelected

    override fun getPreferredFocusedComponent() = myPasswordInput

    override fun createSouthAdditionalPanel(): JPanel {
        return JBUI.Panels.simplePanel(myRememberCheckBox)
    }

    override fun createCenterPanel(): JComponent {
        return myPanel.apply {
            preferredSize = JBDimension(300, -1)
        }
    }
}
