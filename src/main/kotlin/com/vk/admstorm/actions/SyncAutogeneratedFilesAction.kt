package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.vk.admstorm.git.sync.files.RemoteFileManager

class SyncAutogeneratedFilesAction : AdmActionBase() {
    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        RemoteFileManager(e.project!!).doUpdateAutogeneratedFiles()
    }
}
