package com.vk.admstorm.configuration.phpunit

import com.intellij.execution.configurations.LocatableRunConfigurationOptions

class RemotePhpUnitConfigurationOptions : LocatableRunConfigurationOptions() {
    private val myIsDirectoryScope = property(true).provideDelegate(this, "phpUnitIsDirectoryScope")
    private val myIsClassScope = property(false).provideDelegate(this, "phpUnitIsClassScope")
    private val myIsMethodScope = property(false).provideDelegate(this, "phpUnitIsMethodScope")

    private val myDirectory = string("").provideDelegate(this, "phpUnitDirectory")
    private val myClass = string("").provideDelegate(this, "phpUnitClass")
    private val myMethod = string("").provideDelegate(this, "phpUnitMethod")
    private val myFile = string("").provideDelegate(this, "phpUnitFile")

    private val myUseParatest = property(false).provideDelegate(this, "phpUnitUseParatest")
    private val myConfigPath = string("").provideDelegate(this, "phpUnitConfigPath")
    private val myPhpUnitPath = string("").provideDelegate(this, "phpUnitPhpUnitPath")
    private val myWorkingDir = string("").provideDelegate(this, "phpUnitWorkingDir")
    private val myAdditionalParameters = string("").provideDelegate(this, "phpUnitAdditionalParameters")
    private val myIsApiTest = property(false).provideDelegate(this, "phpUnitIsApiTest")
    private val myIsPackageTest = property(false).provideDelegate(this, "phpUnitIsPackageTest")

    var isDirectoryScope: Boolean
        get() = myIsDirectoryScope.getValue(this)
        set(value) {
            myIsDirectoryScope.setValue(this, value)
        }

    var isClassScope: Boolean
        get() = myIsClassScope.getValue(this)
        set(value) {
            myIsClassScope.setValue(this, value)
        }

    var isMethodScope: Boolean
        get() = myIsMethodScope.getValue(this)
        set(value) {
            myIsMethodScope.setValue(this, value)
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

    var method: String
        get() = myMethod.getValue(this) ?: ""
        set(value) {
            myMethod.setValue(this, value)
        }

    var filename: String
        get() = myFile.getValue(this) ?: ""
        set(value) {
            myFile.setValue(this, value)
        }

    var useParatest: Boolean
        get() = myUseParatest.getValue(this)
        set(value) {
            myUseParatest.setValue(this, value)
        }

    var additionalParameters: String
        get() = myAdditionalParameters.getValue(this) ?: ""
        set(value) {
            myAdditionalParameters.setValue(this, value)
        }

    var configPath: String
        get() = myConfigPath.getValue(this) ?: ""
        set(value) {
            myConfigPath.setValue(this, value)
        }

    var phpUnitPath: String
        get() = myPhpUnitPath.getValue(this) ?: ""
        set(value) {
            myPhpUnitPath.setValue(this, value)
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
