package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.vk.admstorm.AdmService
import com.vk.admstorm.AdmStormStartupActivity
import com.vk.admstorm.ssh.SshConnectionService

class ConnectToAdmViaSshAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        SshConnectionService.getInstance(e.project!!).connect {
            AdmStormStartupActivity.getInstance(e.project!!).afterConnectionTasks(e.project!!)
        }
    }

    override fun update(e: AnActionEvent) {
        if (e.project == null || !AdmService.getInstance(e.project!!).needBeEnabled()) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        e.presentation.text = when (SshConnectionService.getInstance(e.project!!).isConnected()) {
            true -> "Reconnect to Adm via SSH"
            else -> "Connect to Adm via SSH"
        }
    }
}
