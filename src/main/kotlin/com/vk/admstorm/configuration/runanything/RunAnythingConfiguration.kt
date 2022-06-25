package com.vk.admstorm.configuration.runanything

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.vk.admstorm.utils.extensions.readString
import com.vk.admstorm.utils.extensions.writeString
import org.jdom.Element

open class RunAnythingConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    LocatableConfigurationBase<RunAnythingConfigurationOptions?>(project, factory, name) {

    override fun getOptions() = super.getOptions() as RunAnythingConfigurationOptions

    var parameters: String
        get() = options.parameters
        set(parameters) {
            options.parameters = parameters
        }

    var envVariables: String
        get() = options.envVariables
        set(envVariables) {
            options.envVariables = envVariables
        }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("runAnythingParameters", parameters)
        element.writeString("runAnythingEnvVariables", envVariables)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readString("runAnythingParameters")?.let { parameters = it }
        element.readString("runAnythingEnvVariables")?.let { envVariables = it }
    }

    override fun getConfigurationEditor() = RunAnythingConfigurationEditor()

    override fun checkConfiguration() {}

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment) =
        RunAnythingConfigurationRunState(executionEnvironment, this)
}
