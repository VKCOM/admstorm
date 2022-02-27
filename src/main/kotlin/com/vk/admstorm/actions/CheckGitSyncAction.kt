package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.vk.admstorm.git.sync.SyncChecker
import com.vk.admstorm.ui.MessageDialog
import com.vk.admstorm.utils.ServerNameProvider

class CheckGitSyncAction : AdmActionBase() {
    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        SyncChecker.getInstance(e.project!!).doCheckSyncSilentlyTask({}) {
            MessageDialog.showSuccess(
                "Local and ${ServerNameProvider.name()} repositories are completely synchronized",
                "Synchronized"
            )
        }
    }
}
