package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.vk.admstorm.YarnWatchService

class YarnWatchAction : AdmActionBase() {
    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        YarnWatchService.getInstance(e.project!!).toggle()
    }

    override fun beforeUpdate(e: AnActionEvent) {
        val project = e.project ?: return
        val working = YarnWatchService.getInstance(project).isRunning()
        if (working) {
            e.presentation.text = "Stop Yarn Watch"
        } else {
            e.presentation.text = "Start Yarn Watch"
        }
    }
}
