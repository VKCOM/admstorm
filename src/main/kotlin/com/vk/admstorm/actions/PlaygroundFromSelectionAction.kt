package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.vk.admstorm.playground.KphpPlaygroundWindow
import com.vk.admstorm.ui.AdmIcons
import javax.swing.JComponent

class PlaygroundFromSelectionAction : AdmActionBase() {
    class OptionsDialog(project: Project) : DialogWrapper(project, true, IdeModalityType.PROJECT) {
        private val myImportClassesCheckBox = JBCheckBox("Auto import classes", true)
        private val mySurroundWithVarDumpCheckBox = JBCheckBox("Surround with var_dump() call")

        init {
            title = "KPHP Playground Options"
            init()
        }

        fun isSurroundWithVarDump() = mySurroundWithVarDumpCheckBox.isSelected
        fun isImportClasses() = myImportClassesCheckBox.isSelected

        override fun createCenterPanel(): JComponent {
            return JBUI.Panels.simplePanel()
                .addToTop(myImportClassesCheckBox)
                .addToBottom(mySurroundWithVarDumpCheckBox)
                .apply {
                    preferredSize = JBDimension(300, 47)
                }
        }
    }

    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val file = e.getRequiredData(CommonDataKeys.PSI_FILE)
        val primaryCaret = editor.caretModel.primaryCaret

        val dialog = OptionsDialog(e.project!!)
        if (!dialog.showAndGet()) {
            return
        }

        val code = if (dialog.isSurroundWithVarDump()) {
            "var_dump(${primaryCaret.selectedText!!.trim().removeSuffix(";")});"
        } else {
            primaryCaret.selectedText!!
        }

        KphpPlaygroundWindow(e.project!!).withCode(file, dialog.isImportClasses(), code)
    }

    override fun beforeUpdate(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        val primaryCaret = editor?.caretModel?.primaryCaret
        val hasSelection = primaryCaret?.hasSelection()

        e.presentation.isEnabledAndVisible = file != null && project != null && editor != null && hasSelection == true
        e.presentation.icon = AdmIcons.General.Kphp
    }
}
