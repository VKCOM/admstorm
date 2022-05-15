package com.vk.admstorm.configuration.kbench

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
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

    var scope: KBenchScope
        get() = options.scope
        set(value) {
            options.scope = value
        }

    var className: String
        get() = options.className
        set(value) {
            options.className = value
        }

    var methodName: String
        get() = options.methodName
        set(value) {
            options.methodName = value
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

    var countIteration: Int
        get() = options.countIteration
        set(value) {
            options.countIteration = value
        }

    var benchmarkMemory: Boolean
        get() = options.benchmarkMemory
        set(value) {
            options.benchmarkMemory = value
        }

    var compareClassName: String
        get() = options.compareClassName
        set(value) {
            options.compareClassName = value
        }

    var compareMethodName: String
        get() = options.compareMethodName
        set(value) {
            options.compareMethodName = value
        }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("kbenchScope", scope.name)
        element.writeString("kbenchType", benchType.command)
        element.writeString("kbenchClassName", className)
        element.writeString("kbenchMethodName", methodName)
        element.writeString("kbenchCompareClassName", compareClassName)
        element.writeString("kbenchCompareMethodName", compareMethodName)
        element.writeString("kbenchFilename", filename)
        element.writeString("kbenchCountIterations", countIteration.toString())
        element.writeBool("kbenchBenchmarkMemory", benchmarkMemory)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readString("kbenchScope")?.let { scope = KBenchScope.from(it) }
        element.readString("kbenchType")?.let { benchType = KBenchType.from(it) }
        element.readString("kbenchClassName")?.let { className = it }
        element.readString("kbenchMethodName")?.let { methodName = it }
        element.readString("kbenchCompareClassName")?.let { compareClassName = it }
        element.readString("kbenchCompareMethodName")?.let { compareMethodName = it }
        element.readString("kbenchFilename")?.let { filename = it }
        element.readString("kbenchCountIterations")?.let { countIteration = it.toIntOrNull() ?: 5 }
        element.readBool("kbenchBenchmarkMemory")?.let { benchmarkMemory = it }
    }

    override fun getConfigurationEditor() = KBenchConfigurationEditor(project)

    override fun checkConfiguration() {
        // TODO: add check for class or method?
    }

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState? {
        return KBenchConfigurationRunState(executionEnvironment, this)
    }
}
