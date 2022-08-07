package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.vk.admstorm.playground.KphpPlaygroundWindow
import com.vk.admstorm.ui.AdmIcons

class KphpPlaygroundAction : AdmActionBase() {
    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        KphpPlaygroundWindow(e.project!!).show()
    }

    override fun beforeUpdate(e: AnActionEvent) {
        e.presentation.icon = AdmIcons.General.Kphp
    }
}
