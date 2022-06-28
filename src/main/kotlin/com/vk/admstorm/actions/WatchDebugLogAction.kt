package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.vk.admstorm.services.WatchDebugLogService

class WatchDebugLogAction : AdmActionBase() {
    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        WatchDebugLogService.getInstance(e.project!!).toggle()
    }

    override fun beforeUpdate(e: AnActionEvent) {
        val project = e.project ?: return
        val working = WatchDebugLogService.getInstance(project).isRunning()

        e.presentation.text = if (working) {
            "Stop Watch Log"
        } else {
            "Start Watch Log"
        }
    }
}
