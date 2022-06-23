package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.vk.admstorm.utils.ServerNameProvider
import com.vk.admstorm.utils.extensions.pluginEnabled

class AdmActionsGroup : DefaultActionGroup() {
    override fun update(event: AnActionEvent) {
        event.presentation.text = ServerNameProvider.uppercase()
        event.presentation.isEnabledAndVisible =
            event.project != null && event.project!!.pluginEnabled()
    }
}
