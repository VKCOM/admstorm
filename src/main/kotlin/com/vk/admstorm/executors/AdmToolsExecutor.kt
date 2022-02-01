package com.vk.admstorm.executors

import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistry
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project

class AdmToolsExecutor : Executor() {
    companion object {
        const val TOOL_WINDOW_ID = "AdmToolExecutor"
        private const val EXECUTOR_ID = "AdmTools"

        fun getRunExecutorInstance() = ExecutorRegistry.getInstance().getExecutorById(EXECUTOR_ID)
    }

    override fun getStartActionText() = "Adm Tool"
    override fun getToolWindowId() = TOOL_WINDOW_ID
    override fun getToolWindowIcon() = AllIcons.Toolwindows.ToolWindowRun
    override fun getIcon() = AllIcons.Actions.Execute
    override fun getDisabledIcon() = AllIcons.RunConfigurations.Remote
    override fun getDescription() = null
    override fun getActionName() = "Adm Tool"
    override fun getId() = EXECUTOR_ID
    override fun getContextActionId() = "AdmTools"
    override fun getHelpId() = null

    /**
     * Remove it from the list of configurations in this way,
     * since we use it manually.
     */
    override fun isApplicable(project: Project) = false
}
