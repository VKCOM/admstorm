package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.vk.admstorm.AdmService
import com.vk.admstorm.ssh.SshConnectionService

/**
 * Base class for all plugin actions that require SSH connection.
 *
 * To use, implement the [actionWithConnectionPerformed] method,
 * which has the same signature as the default [actionPerformed] method.
 *
 * If the project is not vkcom, then this action will be disabled and invisible.
 */
abstract class AdmActionBase : AnAction() {
    final override fun actionPerformed(e: AnActionEvent) {
        if (e.project == null || !SshConnectionService.getInstance(e.project!!).isConnectedOrWarning()) {
            return
        }

        actionWithConnectionPerformed(e)
    }

    override fun update(e: AnActionEvent) {
        beforeUpdate(e)

        if (e.project == null || !AdmService.getInstance(e.project!!).needBeEnabled()) {
            e.presentation.isEnabledAndVisible = false
        }
    }

    open fun beforeUpdate(e: AnActionEvent) {}

    /**
     * Implement this method to provide your action handler.
     *
     * @param e Carries information on the invocation place
     */
    abstract fun actionWithConnectionPerformed(e: AnActionEvent)
}
