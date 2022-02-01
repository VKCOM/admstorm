package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.vk.admstorm.env.Env
import com.vk.admstorm.git.sync.SyncChecker
import com.vk.admstorm.ui.MessageDialog

class CheckGitSyncAction : AdmActionBase() {
    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        SyncChecker.getInstance(e.project!!).doCheckSyncSilentlyTask({}) {
            MessageDialog.showSuccess(
                "Local and ${Env.data.serverName} repositories are completely synchronized",
                "Synchronized"
            )
        }
    }
}
