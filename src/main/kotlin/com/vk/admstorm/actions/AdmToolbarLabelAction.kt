package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ToolbarLabelAction
import com.vk.admstorm.AdmService

class AdmToolbarLabelAction : ToolbarLabelAction() {
    override fun update(e: AnActionEvent) {
        super.update(e)

        if (e.project == null || !AdmService.getInstance(e.project!!).needBeEnabled()) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        e.presentation.text = "Adm:"
    }
}
