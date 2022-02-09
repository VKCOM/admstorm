package com.vk.admstorm.actions.git.checkout

import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import com.vk.admstorm.AdmStormStartupActivity
import com.vk.admstorm.actions.git.PushToGitlabAction
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.ssh.SshConnectionService

class GitCustomPushCheckinHandlerFactory : CheckinHandlerFactory() {
    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return object : CheckinHandler() {
            override fun checkinSuccessful() {
                if (!commitContext.isPushToGitlabAfterCommit) {
                    return
                }

                if (!SshConnectionService.getInstance(panel.project).isConnected()) {
                    AdmWarningNotification("To perform push, connect via SSH to the required server")
                        .withTitle("No connection with server")
                        .withActions(
                            AdmNotification.Action("Connect...") { e, notification ->
                                notification.expire()
                                SshConnectionService.getInstance(panel.project).connect {
                                    AdmStormStartupActivity.afterConnectionTasks(e.project!!)
                                }
                            }
                        )
                        .show()
                    return
                }

                PushToGitlabAction().runAction(panel.project)
            }
        }
    }
}
