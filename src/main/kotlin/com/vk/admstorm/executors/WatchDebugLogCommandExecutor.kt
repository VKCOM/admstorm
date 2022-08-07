package com.vk.admstorm.executors

import com.intellij.openapi.project.Project
import com.vk.admstorm.services.WatchDebugLogService
import com.vk.admstorm.ui.AdmIcons

class WatchDebugLogCommandExecutor(project: Project, private val command: String) :
    BaseRemoteExecutor(project, "Watch debug log") {

    private val service
        get() = WatchDebugLogService.getInstance(project)

    override fun onFinish() {
        service.setWorking(false)
    }

    override fun onStop() {
        service.setWorking(false)
    }

    override fun onStopBeforeRerun() {
        service.setWorking(false)
    }

    override fun onRerun() {
        service.setWorking(true)
    }

    override fun layoutName() = "Watch debug log"

    override fun tabName() = "watch"

    override fun command() = command

    override fun icon() = AdmIcons.General.Logs

    override fun executorInstance() = WatchDebugLogExecutor.getRunExecutorInstance()

    override fun toolWindowId() = WatchDebugLogExecutor.TOOL_WINDOW_ID

    override fun runnerTitle() = "Watch Debug Log"

    override fun runnerId() = "WatchDebugLogCommandExecutor"
}
