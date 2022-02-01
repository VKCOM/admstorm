package com.vk.admstorm.configuration.kphp

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.vk.admstorm.utils.readBool
import com.vk.admstorm.utils.readString
import com.vk.admstorm.utils.writeBool
import com.vk.admstorm.utils.writeString
import org.jdom.Element

open class KphpConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    LocatableConfigurationBase<KphpConfigurationOptions?>(project, factory, name) {

    override fun getOptions() = super.getOptions() as KphpConfigurationOptions

    var runType: KphpRunType
        get() = options.runType
        set(runType) {
            options.runType = runType
        }

    var parameters: String
        get() = options.parameters
        set(parameters) {
            options.parameters = parameters
        }

    var runScriptWithPhp: Boolean
        get() = options.runScriptWithPhp
        set(runScriptWithPhp) {
            options.runScriptWithPhp = runScriptWithPhp
        }

    var envVariables: String
        get() = options.envVariables
        set(envVariables) {
            options.envVariables = envVariables
        }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("runType", runType.name)
        element.writeString("parameters", parameters)
        element.writeBool("runScriptWithPhp", runScriptWithPhp)
        element.writeString("envVariables", envVariables)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readString("runType")?.let { runType = KphpRunType.from(it) }
        element.readString("parameters")?.let { parameters = it }
        element.readBool("runScriptWithPhp")?.let { runScriptWithPhp = it }
        element.readString("envVariables")?.let { envVariables = it }
    }

    override fun getConfigurationEditor() = KphpConfigurationEditor()

    override fun checkConfiguration() {}

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment) =
        KphpConfigurationRunState(executionEnvironment, this)
}
