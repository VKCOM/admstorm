package com.vk.admstorm.configuration.yarnwatch

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project

open class YarnWatchConfiguration(project: Project, factory: ConfigurationFactory?, name: String?) :
    RunConfigurationBase<YarnWatchConfigurationOptions?>(project, factory, name) {

    override fun getConfigurationEditor() = YarnWatchConfigurationEditor()

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState? {
        return YarnWatchConfigurationRunState(project)
    }
}
