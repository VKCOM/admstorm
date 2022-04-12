package com.vk.admstorm.configuration.phpunit

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.vk.admstorm.utils.extensions.readBool
import com.vk.admstorm.utils.extensions.readString
import com.vk.admstorm.utils.extensions.writeBool
import com.vk.admstorm.utils.extensions.writeString
import org.jdom.Element

open class RemotePhpUnitConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    LocatableConfigurationBase<RemotePhpUnitConfigurationOptions?>(project, factory, name) {

    override fun getOptions() = super.getOptions() as RemotePhpUnitConfigurationOptions

    var isDirectoryScope: Boolean
        get() = options.isDirectoryScope
        set(value) {
            options.isDirectoryScope = value
        }

    var isClassScope: Boolean
        get() = options.isClassScope
        set(value) {
            options.isClassScope = value
        }

    var isMethodScope: Boolean
        get() = options.isMethodScope
        set(value) {
            options.isMethodScope = value
        }

    var directory: String
        get() = options.directory
        set(value) {
            options.directory = value
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

    var useParatest: Boolean
        get() = options.useParatest
        set(value) {
            options.useParatest = value
        }

    var additionalParameters: String
        get() = options.additionalParameters
        set(value) {
            options.additionalParameters = value
        }

    var configPath: String
        get() = options.configPath
        set(value) {
            options.configPath = value
        }

    var workingDir: String
        get() = options.workingDir
        set(value) {
            options.workingDir = value
        }

    var isApiTest: Boolean
        get() = options.isApiTest
        set(value) {
            options.isApiTest = value
        }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeBool("isDirectoryScope", isDirectoryScope)
        element.writeBool("isClassScope", isClassScope)
        element.writeBool("isMethodScope", isMethodScope)
        element.writeString("directory", directory)
        element.writeString("className", className)
        element.writeString("method", method)
        element.writeString("filename", filename)
        element.writeBool("useParatest", useParatest)
        element.writeString("configPath", configPath)
        element.writeString("workingDir", workingDir)
        element.writeString("additionalParameters", additionalParameters)
        element.writeBool("isApiTest", isApiTest)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readBool("isDirectoryScope")?.let { isDirectoryScope = it }
        element.readBool("isClassScope")?.let { isClassScope = it }
        element.readBool("isMethodScope")?.let { isMethodScope = it }
        element.readString("directory")?.let { directory = it }
        element.readString("className")?.let { className = it }
        element.readString("method")?.let { method = it }
        element.readString("filename")?.let { filename = it }
        element.readBool("useParatest")?.let { useParatest = it }
        element.readString("configPath")?.let { configPath = it }
        element.readString("workingDir")?.let { workingDir = it }
        element.readString("additionalParameters")?.let { additionalParameters = it }
        element.readBool("isApiTest")?.let { isApiTest = it }
    }

    override fun getConfigurationEditor() = RemotePhpUnitConfigurationEditor(project)

    override fun checkConfiguration() {}

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState? {
        return RemotePhpUnitConfigurationRunState(executionEnvironment, this)
    }
}
