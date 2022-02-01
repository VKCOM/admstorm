package com.vk.admstorm.configuration.phplinter

import com.intellij.execution.configurations.LocatableRunConfigurationOptions

class PhpLinterConfigurationOptions : LocatableRunConfigurationOptions() {
    private val myParameters = string("").provideDelegate(this, "phpLinterParameters")
    private val myRunAsInTeamcity = property(false).provideDelegate(this, "phpLinterRunAsInTeamcity")

    var parameters: String
        get() = myParameters.getValue(this) ?: ""
        set(value) {
            myParameters.setValue(this, value)
        }

    var runAsInTeamcity: Boolean
        get() = myRunAsInTeamcity.getValue(this)
        set(value) {
            myRunAsInTeamcity.setValue(this, value)
        }
}
