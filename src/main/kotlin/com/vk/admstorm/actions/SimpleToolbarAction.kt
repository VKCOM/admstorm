package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.Icon

class SimpleToolbarAction(
    text: String,
    icon: Icon,
    private val action: (AnActionEvent) -> Unit
) : ActionToolbarFastEnableAction(text, icon) {

    override fun actionPerformed(e: AnActionEvent) {
        action(e)
    }
}
