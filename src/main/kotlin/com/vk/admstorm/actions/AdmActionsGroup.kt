package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.vk.admstorm.utils.ServerNameProvider
import com.vk.admstorm.utils.extensions.pluginEnabled

class AdmActionsGroup : DefaultActionGroup() {
    override fun update(event: AnActionEvent) {
        val serverName = ServerNameProvider.uppercase()
        event.presentation.text = serverName.replaceFirstChar { it.uppercaseChar() }
        event.presentation.isEnabledAndVisible =
            event.project != null && event.project!!.pluginEnabled()
    }
}
