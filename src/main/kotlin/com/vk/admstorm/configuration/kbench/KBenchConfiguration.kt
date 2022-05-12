package com.vk.admstorm.configuration.kbench

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.vk.admstorm.utils.extensions.readBool
import com.vk.admstorm.utils.extensions.readString
import com.vk.admstorm.utils.extensions.writeBool
import com.vk.admstorm.utils.extensions.writeString
import org.jdom.Element

open class KBenchConfiguration(project: Project, factory: ConfigurationFactory?, name: String?) :
    RunConfigurationBase<KBenchConfigurationOptions?>(project, factory, name) {

    override fun getOptions() = super.getOptions() as KBenchConfigurationOptions

    var isMethodScope: Boolean
        get() = options.isMethodScope
        set(value) {
            options.isMethodScope = value
        }

    var className: String
        get() = options.className
        set(value) {
            options.className = value
        }

    var method: String
        get() = options.method
        set(value) {
            options.method = value
        }

    var filename: String
        get() = options.filename
        set(value) {
            options.filename = value
        }

    var benchType: KBenchType
        get() = options.benchType
        set(value) {
            options.benchType = value
        }

    var countRuns: String
        get() = options.countRuns
        set(value) {
            options.countRuns = value
        }

    var benchmem: Boolean
        get() = options.benchmem
        set(value) {
            options.benchmem = value
        }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeBool("kphpBenchIsMethodScope", isMethodScope)
        element.writeString("kphpBenchClassName", className)
        element.writeString("kphpBenchMethod", method)
        element.writeString("kphpBenchFilename", filename)
        element.writeString("kphpBenchType", benchType.command)
        element.writeString("kphpCountRuns", countRuns)
        element.writeBool("kphpBenchBenchmem", benchmem)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readBool("kphpBenchIsMethodScope")?.let { isMethodScope = it }
        element.readString("kphpBenchClassName")?.let { className = it }
        element.readString("kphpBenchMethod")?.let { method = it }
        element.readString("kphpBenchFilename")?.let { filename = it }
        element.readString("kphpBenchType")?.let { benchType = KBenchType.from(it) }
        element.readString("kphpCountRuns")?.let { countRuns = it }
        element.readBool("kphpBenchBenchmem")?.let { benchmem = it }
    }

    override fun getConfigurationEditor() = KBenchConfigurationEditor(project)

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
