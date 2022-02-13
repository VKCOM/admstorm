package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.playground.KphpPlaygroundWindow
import com.vk.admstorm.ui.MyIcons
import com.vk.admstorm.utils.MyUtils

class PlaygroundFromSelectionAction : AdmActionBase() {
    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val primaryCaret = editor.caretModel.primaryCaret
        val selectedText = primaryCaret.selectedText!!

        KphpPlaygroundWindow(e.project!!).withCode(selectedText)
    }

    override fun beforeUpdate(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        val primaryCaret = editor?.caretModel?.primaryCaret
        val hasSelection = primaryCaret?.hasSelection()

        e.presentation.isEnabledAndVisible = file != null && project != null && editor != null && hasSelection == true
        e.presentation.icon = MyIcons.kphp
    }
}
