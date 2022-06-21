package com.vk.admstorm.configuration.phpunit

import com.intellij.execution.configurations.LocatableRunConfigurationOptions

class RemotePhpUnitConfigurationOptions : LocatableRunConfigurationOptions() {
    private val myScope = string("").provideDelegate(this, "phpUnitScope")

    private val myDirectory = string("").provideDelegate(this, "phpUnitDirectory")
    private val myClass = string("").provideDelegate(this, "phpUnitClass")
    private val myMethod = string("").provideDelegate(this, "phpUnitMethod")
    private val myFile = string("").provideDelegate(this, "phpUnitFile")

    private val myPhpUnitConfig = string("").provideDelegate(this, "phpUnitConfig")
    private val myPhpUnitExe = string("").provideDelegate(this, "phpUnitExe")
    private val myWorkingDir = string("").provideDelegate(this, "phpUnitWorkingDir")
    private val myAdditionalParameters = string("").provideDelegate(this, "phpUnitAdditionalParameters")
    private val myIsApiTest = property(false).provideDelegate(this, "phpUnitIsApiTest")
    private val myIsPackageTest = property(false).provideDelegate(this, "phpUnitIsPackageTest")

    var scope: PhpUnitScope
        get() = PhpUnitScope.from(myScope.getValue(this) ?: "")
        set(value) {
            myScope.setValue(this, value.name)
        }

    var directory: String
        get() = myDirectory.getValue(this) ?: ""
        set(value) {
            myDirectory.setValue(this, value)
        }

    var className: String
        get() = myClass.getValue(this) ?: ""
        set(value) {
            myClass.setValue(this, value)
        }

    var methodName: String
        get() = myMethod.getValue(this) ?: ""
        set(value) {
            myMethod.setValue(this, value)
        }

    var filename: String
        get() = myFile.getValue(this) ?: ""
        set(value) {
            myFile.setValue(this, value)
        }

    var additionalParameters: String
        get() = myAdditionalParameters.getValue(this) ?: ""
        set(value) {
            myAdditionalParameters.setValue(this, value)
        }

    var phpUnitConfig: String
        get() = myPhpUnitConfig.getValue(this) ?: ""
        set(value) {
            myPhpUnitConfig.setValue(this, value)
        }

    var phpUnitExe: String
        get() = myPhpUnitExe.getValue(this) ?: ""
        set(value) {
            myPhpUnitExe.setValue(this, value)
        }

    var workingDir: String
        get() = myWorkingDir.getValue(this) ?: ""
        set(value) {
            myWorkingDir.setValue(this, value)
        }

    var isApiTest: Boolean
        get() = myIsApiTest.getValue(this)
        set(value) {
            myIsApiTest.setValue(this, value)
        }

    var isPackageTest: Boolean
        get() = myIsPackageTest.getValue(this)
        set(value) {
            myIsPackageTest.setValue(this, value)
        }
}
