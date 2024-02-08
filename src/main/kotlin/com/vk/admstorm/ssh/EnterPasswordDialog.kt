package com.vk.admstorm.ssh

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBDimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JLabel
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

    private val warningLabel = JLabel().apply { foreground = JBColor.RED }

    init {
        title = "Enter PIN"

        init()

        myPasswordInput.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                if (getPassword().any { it in 'А'..'я' || it == 'ё' || it == 'Ё' }) {
                    warningLabel.text = "PIN should contain only English characters and numbers!"
                } else {
                    warningLabel.text = ""
                }
            }
        })
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
                label("Enter PIN for Yubikey:")
            }.topGap(TopGap.NONE)

            row {
                cell(myPasswordInput)
                    .align(AlignX.FILL)
            }.bottomGap(BottomGap.NONE)

            row{
                cell(warningLabel)
            }.bottomGap(BottomGap.NONE)

        }.apply {
            preferredSize = JBDimension(300, -1)
        }
    }
}
