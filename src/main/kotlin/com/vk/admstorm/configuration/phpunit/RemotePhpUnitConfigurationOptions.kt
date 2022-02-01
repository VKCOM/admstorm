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
    private val myAdditionalParameters = string("").provideDelegate(this, "phpUnitAdditionalParameters")

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
}
