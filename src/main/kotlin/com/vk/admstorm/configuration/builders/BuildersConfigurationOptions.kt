package com.vk.admstorm.configuration.builders

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

class BuildersConfigurationOptions : RunConfigurationOptions() {
    private val myParameters: StoredProperty<String?> = string("").provideDelegate(this, "buildersParameters")

    var parameters: String
        get() = myParameters.getValue(this) ?: ""
        set(value) {
            myParameters.setValue(this, value)
        }
}
