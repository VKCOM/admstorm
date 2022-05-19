package com.vk.admstorm.configuration.php

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

class RemotePhpConfigurationOptions : RunConfigurationOptions() {
    private val myScriptName: StoredProperty<String?> = string("")
        .provideDelegate(this, "scriptName")

    var scriptName: String
        get() = myScriptName.getValue(this) ?: ""
        set(value) {
            myScriptName.setValue(this, value)
        }
}
