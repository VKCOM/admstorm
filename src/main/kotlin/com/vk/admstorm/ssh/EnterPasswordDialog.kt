package com.vk.admstorm.ssh

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBDimension
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

    private var myPasswordInput = JBPasswordField()
    private val myRememberCheckBox = JBCheckBox("Remember", true)

    fun getPassword() = String(myPasswordInput.password)
    fun isRemember() = myRememberCheckBox.isSelected

    init {
        title = "Enter Password"

        init()
    }

    override fun getPreferredFocusedComponent() = myPasswordInput

    override fun createSouthAdditionalPanel(): JPanel {
        return panel {
            row {
                cell(myRememberCheckBox)
            }
        }
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label("Enter password for Yubikey:")
            }.topGap(TopGap.NONE)

            row {
                cell(myPasswordInput)
                    .align(AlignX.FILL)
            }.bottomGap(BottomGap.NONE)
        }.apply {
            preferredSize = JBDimension(300, -1)
        }
    }
}
