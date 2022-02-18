package com.vk.admstorm.configuration.builders

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.vk.admstorm.utils.extensions.readString
import com.vk.admstorm.utils.extensions.writeString
import org.jdom.Element

open class BuildersConfiguration(project: Project, factory: ConfigurationFactory?, name: String?) :
    RunConfigurationBase<BuildersConfigurationOptions?>(project, factory, name) {

    override fun getOptions() = super.getOptions() as BuildersConfigurationOptions

    var parameters: String
        get() = options.parameters
        set(value) {
            options.parameters = value
        }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("buildersParameters", parameters)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readString("buildersParameters")?.let { parameters = it }
    }

    override fun getConfigurationEditor() = BuildersConfigurationEditor()

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState? {
        return BuildersConfigurationRunState(executionEnvironment, this)
    }
}
