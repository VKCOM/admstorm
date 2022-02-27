package com.vk.admstorm.configuration.runanything

import com.intellij.execution.configurations.LocatableRunConfigurationOptions

class RunAnythingConfigurationOptions : LocatableRunConfigurationOptions() {
    private val myParameters = string("").provideDelegate(this, "runAnythingRunParameters")
    private val myEnvVariables = string("").provideDelegate(this, "runAnythingEnvVariables")

    var parameters: String
        get() = myParameters.getValue(this) ?: ""
        set(scriptName) {
            myParameters.setValue(this, scriptName)
        }

    var envVariables: String
        get() = myEnvVariables.getValue(this) ?: ""
        set(envVariables) {
            myEnvVariables.setValue(this, envVariables)
        }
}
