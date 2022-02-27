package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.vk.admstorm.AdmService
import com.vk.admstorm.utils.ServerNameProvider

class AdmActionsGroup : DefaultActionGroup() {
    override fun update(event: AnActionEvent) {
        val serverName = ServerNameProvider.uppercase()
        event.presentation.text = serverName.replaceFirstChar { it.uppercaseChar() }
        event.presentation.isEnabledAndVisible =
            event.project != null && AdmService.getInstance(event.project!!).needBeEnabled()
    }
}
