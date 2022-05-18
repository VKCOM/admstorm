package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ToolbarLabelAction
import com.vk.admstorm.utils.extensions.pluginEnabled

class AdmToolbarLabelAction : ToolbarLabelAction() {
    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project

        if (project == null || !project.pluginEnabled()) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        e.presentation.text = "Adm:"
    }
}
