package com.vk.admstorm.configuration.kphp

import com.intellij.execution.configurations.LocatableRunConfigurationOptions

class KphpConfigurationOptions : LocatableRunConfigurationOptions() {
    private val myRunType = string("Bu").provideDelegate(this, "kphpRunType")
    private val myParameters = string("").provideDelegate(this, "kphpRunParameters")
    private val myRunScriptWithPhp = property(true).provideDelegate(this, "kphpRunScriptWithPhp")
    private val myEnvVariables = string("").provideDelegate(this, "kphpEnvVariables")

    var runType: KphpRunType
        get() = KphpRunType.from(myRunType.getValue(this)!!)
        set(value) {
            myRunType.setValue(this, value.name)
        }

    var parameters: String
        get() = myParameters.getValue(this) ?: ""
        set(value) {
            myParameters.setValue(this, value)
        }

    var runScriptWithPhp: Boolean
        get() = myRunScriptWithPhp.getValue(this)
        set(value) {
            myRunScriptWithPhp.setValue(this, value)
        }

    var envVariables: String
        get() = myEnvVariables.getValue(this) ?: ""
        set(value) {
            myEnvVariables.setValue(this, value)
        }
}
