package com.vk.admstorm.executors

import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistry
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.vk.admstorm.ui.AdmIcons

class WatchDebugLogExecutor : Executor() {
    companion object {
        const val TOOL_WINDOW_ID = "WatchDebugLogExecutor"
        private const val EXECUTOR_ID = "WatchDebugLog"

        fun getRunExecutorInstance() = ExecutorRegistry.getInstance().getExecutorById(EXECUTOR_ID)
    }

    override fun getStartActionText() = "Process"
    override fun getToolWindowId() = TOOL_WINDOW_ID
    override fun getToolWindowIcon() = AdmIcons.General.Logs
    override fun getIcon() = AdmIcons.General.Logs
    override fun getDisabledIcon() = AllIcons.RunConfigurations.Remote
    override fun getDescription() = null
    override fun getActionName() = "Watch Debug Log"
    override fun getId() = EXECUTOR_ID
    override fun getContextActionId() = "WatchDebugLog"
    override fun getHelpId() = null

    /**
     * Remove it from the list of configurations in this way,
     * since we use it manually.
     */
    override fun isApplicable(project: Project) = false
}
