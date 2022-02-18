package com.vk.admstorm.configuration.phplinter

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.vk.admstorm.utils.extensions.readBool
import com.vk.admstorm.utils.extensions.readString
import com.vk.admstorm.utils.extensions.writeBool
import com.vk.admstorm.utils.extensions.writeString
import org.jdom.Element

class PhpLinterConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    LocatableConfigurationBase<PhpLinterConfigurationOptions>(project, factory, name) {

    override fun getOptions() = super.getOptions() as PhpLinterConfigurationOptions

    var parameters: String
        get() = options.parameters
        set(value) {
            options.parameters = value
        }

    var runAsInTeamcity: Boolean
        get() = options.runAsInTeamcity
        set(value) {
            options.runAsInTeamcity = value
        }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("parameters", parameters)
        element.writeBool("runAsInTeamcity", runAsInTeamcity)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readString("parameters")?.let { parameters = it }
        element.readBool("runAsInTeamcity")?.let { runAsInTeamcity = it }
    }

    override fun getConfigurationEditor() = PhpLinterConfigurationEditor()

    override fun checkConfiguration() {}

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment) =
        PhpLinterConfigurationRunState(executionEnvironment, this)
}
