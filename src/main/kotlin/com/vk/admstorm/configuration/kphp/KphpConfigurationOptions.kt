package com.vk.admstorm.configuration.kphp

import com.intellij.execution.configurations.LocatableRunConfigurationOptions

class KphpConfigurationOptions : LocatableRunConfigurationOptions() {
    private val myRunType = string("Bu").provideDelegate(this, "kphpRunType")
    private val myParameters = string("").provideDelegate(this, "kphpRunParameters")
    private val myRunScriptWithPhp = property(true).provideDelegate(this, "kphpRunScriptWithPhp")
    private val myEnvVariables = string("").provideDelegate(this, "kphpEnvVariables")

    var runType: KphpRunType
        get() = KphpRunType.from(myRunType.getValue(this)!!)
        set(runType) {
            myRunType.setValue(this, runType.name)
        }

    var parameters: String
        get() = myParameters.getValue(this) ?: ""
        set(scriptName) {
            myParameters.setValue(this, scriptName)
        }

    var runScriptWithPhp: Boolean
        get() = myRunScriptWithPhp.getValue(this)
        set(runScriptWithPhp) {
            myRunScriptWithPhp.setValue(this, runScriptWithPhp)
        }

    var envVariables: String
        get() = myEnvVariables.getValue(this) ?: ""
        set(envVariables) {
            myEnvVariables.setValue(this, envVariables)
        }
}
