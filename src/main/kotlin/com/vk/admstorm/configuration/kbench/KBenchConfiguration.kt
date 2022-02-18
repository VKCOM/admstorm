package com.vk.admstorm.configuration.kbench

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.vk.admstorm.utils.extensions.readString
import com.vk.admstorm.utils.extensions.writeString
import org.jdom.Element

open class KBenchConfiguration(project: Project, factory: ConfigurationFactory?, name: String?) :
    RunConfigurationBase<KBenchConfigurationOptions?>(project, factory, name) {

    override fun getOptions() = super.getOptions() as KBenchConfigurationOptions

    var scriptName: String
        get() = options.scriptName
        set(scriptName) {
            options.scriptName = scriptName
        }

    var benchType: KBenchType
        get() = options.benchType
        set(benchType) {
            options.benchType = benchType
        }

    var countRuns: String
        get() = options.countRuns
        set(countRuns) {
            options.countRuns = countRuns
        }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("kphpScriptName", scriptName)
        element.writeString("kphpBenchType", benchType.command)
        element.writeString("kphpCountRuns", countRuns)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readString("kphpScriptName")?.let { scriptName = it }
        element.readString("kphpBenchType")?.let { benchType = KBenchType.from(it) }
        element.readString("kphpCountRuns")?.let { countRuns = it }
    }

    override fun getConfigurationEditor() = KBenchConfigurationEditor()

    override fun checkConfiguration() {
        try {
            countRuns.toInt()
        } catch (e: NumberFormatException) {
            throw RuntimeConfigurationError("'count' field value must be an integer")
        }
    }

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState? {
        return KBenchConfigurationRunState(executionEnvironment, this)
    }
}
