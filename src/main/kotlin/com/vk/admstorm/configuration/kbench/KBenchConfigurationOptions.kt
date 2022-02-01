package com.vk.admstorm.configuration.kbench

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

class KBenchConfigurationOptions : RunConfigurationOptions() {
    private val myScriptName: StoredProperty<String?> = string("").provideDelegate(this, "scriptName")
    private val myBenchType: StoredProperty<String?> = string("bench").provideDelegate(this, "benchName")
    private val myCountRuns: StoredProperty<String?> = string("5").provideDelegate(this, "countRuns")

    var scriptName: String
        get() = myScriptName.getValue(this)!!
        set(scriptName) {
            myScriptName.setValue(this, scriptName)
        }

    var benchType: KBenchType
        get() = KBenchType.from(myBenchType.getValue(this)!!)
        set(benchType) {
            myBenchType.setValue(this, benchType.command)
        }

    var countRuns: String
        get() = myCountRuns.getValue(this)!!
        set(scriptName) {
            myCountRuns.setValue(this, scriptName)
        }
}
