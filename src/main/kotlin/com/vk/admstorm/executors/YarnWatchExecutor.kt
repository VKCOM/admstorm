package com.vk.admstorm.executors

import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistry
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.vk.admstorm.ui.MyIcons

class YarnWatchExecutor : Executor() {
    companion object {
        const val TOOL_WINDOW_ID = "YarnWatchExecutor"
        private const val EXECUTOR_ID = "YarnWatch"

        fun getRunExecutorInstance() = ExecutorRegistry.getInstance().getExecutorById(EXECUTOR_ID)
    }

    override fun getStartActionText() = "Process"
    override fun getToolWindowId() = TOOL_WINDOW_ID
    override fun getToolWindowIcon() = AllIcons.Toolwindows.ToolWindowVariableView
    override fun getIcon() = MyIcons.yarn
    override fun getDisabledIcon() = AllIcons.RunConfigurations.Remote
    override fun getDescription() = null
    override fun getActionName() = "Yarn Watch"
    override fun getId() = EXECUTOR_ID
    override fun getContextActionId() = "YarnWatch"
    override fun getHelpId() = null

    /**
     * Remove it from the list of configurations in this way,
     * since we use it manually.
     */
    override fun isApplicable(project: Project) = false
}
