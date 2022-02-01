package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.ui.MessageDialog
import com.vk.admstorm.utils.MyUtils

class CreateHasteAction : AdmActionBase() {
    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE)
        if (file == null) {
            MessageDialog.showError(
                "Unable to create a hastebin for the file because it was not found",
                "File Not Found"
            )
            return
        }

        val text = LoadTextUtil.loadText(file.virtualFile).toString()

        ApplicationManager.getApplication().executeOnPooledThread {
            val link = MyUtils.createHaste(e.project!!, text)
            MyUtils.copyToClipboard(link)
            AdmNotification()
                .withTitle("Link to hastebin copied to clipboard")
                .show()
        }
    }
}
