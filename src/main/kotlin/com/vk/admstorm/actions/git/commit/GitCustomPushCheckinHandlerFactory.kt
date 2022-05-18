package com.vk.admstorm.actions.git.commit

import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import com.vk.admstorm.AdmStormStartupActivity
import com.vk.admstorm.actions.git.PushToGitlabAction
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.settings.AdmStormSettingsState
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.utils.extensions.pluginEnabled

class GitCustomPushCheckinHandlerFactory : CheckinHandlerFactory() {
    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return object : CheckinHandler() {
            override fun checkinSuccessful() {
                if (!panel.project.pluginEnabled()) {
                    return
                }

                if (!commitContext.isPushToGitlabAfterCommit) {
                    if (AdmStormSettingsState.getInstance().autoPushToServerAfterCommit) {
                        pushToServer()
                    }
                    return
                }

                if (!SshConnectionService.getInstance(panel.project).isConnected()) {
                    AdmWarningNotification("To perform push, connect via SSH to the required server")
                        .withTitle("No connection with server")
                        .withActions(
                            AdmNotification.Action("Connect...") { e, notification ->
                                notification.expire()
                                SshConnectionService.getInstance(panel.project).connect {
                                    AdmStormStartupActivity.getInstance(panel.project).afterConnectionTasks(e.project!!)
                                }
                            }
                        )
                        .show()
                    return
                }

                PushToGitlabAction().runAction(panel.project)
            }

            private fun pushToServer() {
                PushToGitlabAction.doForcePushOrNotToServerTask(panel.project, force = true)
            }
        }
    }
}
