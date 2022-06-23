package com.vk.admstorm.executors

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.vk.admstorm.services.WatchDebugLogService
import com.vk.admstorm.ui.MyIcons
import javax.swing.Icon

class WatchDebugLogCommandExecutor(project: Project, command: String) :
    BaseRunnableExecutor(Config(tabName = "watch", layoutName = "Watch debug log", command = command), project) {

    companion object {
        private val LOG = logger<WatchDebugLogCommandExecutor>()
    }

    private val service
        get() = WatchDebugLogService.getInstance(project)

    override fun onReady() {
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

    override fun icon(): Icon = MyIcons.logs

    override fun executorInstance() = WatchDebugLogExecutor.getRunExecutorInstance()
    override fun executorToolWindowId() = WatchDebugLogExecutor.TOOL_WINDOW_ID
    override fun runnerTitle() = "Watch Debug Log"
    override fun runnerId() = "WatchDebugLogCommandExecutor"
}
