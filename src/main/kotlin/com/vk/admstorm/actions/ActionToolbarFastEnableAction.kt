package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.Icon

abstract class ActionToolbarFastEnableAction(
    text: String,
    private val icon: Icon,
    description: String = "",
) : DumbAwareAction(text, description, icon) {

    private var enabled = true

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = enabled
        e.presentation.icon = icon
    }
}
