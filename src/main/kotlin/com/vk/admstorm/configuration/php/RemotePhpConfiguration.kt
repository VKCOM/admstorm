package com.vk.admstorm.configuration.php

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.vk.admstorm.utils.extensions.readString
import com.vk.admstorm.utils.extensions.writeString
import org.jdom.Element

open class RemotePhpConfiguration(project: Project, factory: ConfigurationFactory?, name: String?) :
    RunConfigurationBase<RemotePhpConfigurationOptions?>(project, factory, name) {

    override fun getOptions() = super.getOptions() as RemotePhpConfigurationOptions

    var scriptName: String
        get() = options.scriptName
        set(value) {
            options.scriptName = value
        }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("phpScriptName", scriptName)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readString("phpScriptName")?.let { scriptName = it }
    }

    override fun getConfigurationEditor() = RemotePhpConfigurationEditor()

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState? {
        return RemotePhpConfigurationRunState(executionEnvironment, this)
    }
}
