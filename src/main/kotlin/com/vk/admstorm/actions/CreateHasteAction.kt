package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.services.HastebinService
import com.vk.admstorm.utils.MyUtils
import com.vk.admstorm.utils.MyUtils.executeOnPooledThread

class CreateHasteAction : AdmActionBase() {
    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        val file = e.getRequiredData(CommonDataKeys.PSI_FILE)
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val primaryCaret = editor.caretModel.primaryCaret
        val selectedText = primaryCaret.selectedText

        val copyText = selectedText ?: LoadTextUtil.loadText(file.virtualFile).toString()

        executeOnPooledThread {
            val link = HastebinService.createHaste(e.project!!, copyText)
            MyUtils.copyToClipboard(link)
            AdmNotification()
                .withTitle("Link to hastebin copied to clipboard")
                .show()
        }
    }

    override fun beforeUpdate(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = file != null && project != null && editor != null
    }
}
