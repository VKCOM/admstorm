package com.vk.admstorm.configuration.phpunit

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.vk.admstorm.configuration.WithSshConfiguration
import com.vk.admstorm.utils.extensions.readBool
import com.vk.admstorm.utils.extensions.readString
import com.vk.admstorm.utils.extensions.writeBool
import com.vk.admstorm.utils.extensions.writeString
import org.jdom.Element

open class RemotePhpUnitConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    LocatableConfigurationBase<RemotePhpUnitConfigurationOptions?>(project, factory, name),
    WithSshConfiguration {

    override fun getOptions() = super.getOptions() as RemotePhpUnitConfigurationOptions

    var scope: PhpUnitScope
        get() = options.scope
        set(value) {
            options.scope = value
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

    var phpUnitExe: String
        get() = options.phpUnitExe
        set(value) {
            options.phpUnitExe = value
        }

    var phpUnitConfig: String
        get() = options.phpUnitConfig
        set(value) {
            options.phpUnitConfig = value
        }

    var additionalParameters: String
        get() = options.additionalParameters
        set(value) {
            options.additionalParameters = value
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

    var isPackageTest: Boolean
        get() = options.isPackageTest
        set(value) {
            options.isPackageTest = value
        }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("scope", scope.name)
        element.writeString("directory", directory)
        element.writeString("className", className)
        element.writeString("method", methodName)
        element.writeString("filename", filename)
        element.writeString("configPath", phpUnitConfig)
        element.writeString("phpUnitExe", phpUnitExe)
        element.writeString("workingDir", workingDir)
        element.writeString("additionalParameters", additionalParameters)
        element.writeBool("isApiTest", isApiTest)
        element.writeBool("isPackageTest", isPackageTest)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readString("scope")?.let { scope = PhpUnitScope.from(it) }
        element.readString("directory")?.let { directory = it }
        element.readString("className")?.let { className = it }
        element.readString("method")?.let { methodName = it }
        element.readString("filename")?.let { filename = it }
        element.readString("configPath")?.let { phpUnitConfig = it }
        element.readString("phpUnitExe")?.let { phpUnitExe = it }
        element.readString("workingDir")?.let { workingDir = it }
        element.readString("additionalParameters")?.let { additionalParameters = it }
        element.readBool("isApiTest")?.let { isApiTest = it }
        element.readBool("isPackageTest")?.let { isPackageTest = it }
    }

    override fun getConfigurationEditor() = RemotePhpUnitConfigurationEditor(project)

    override fun checkConfiguration() {}

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState? {
        return RemotePhpUnitConfigurationRunState(executionEnvironment, this)
    }
}
