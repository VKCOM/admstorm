package com.vk.admstorm.configuration.kbench

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

class KBenchConfigurationOptions : RunConfigurationOptions() {
    private val myFilename: StoredProperty<String?> = string("").provideDelegate(this, "kbenchFilename")
    private val myBenchType: StoredProperty<String?> = string("bench").provideDelegate(this, "kbenchBenchName")
    private val myCountRuns: StoredProperty<String?> = string("5").provideDelegate(this, "kbenchCountRuns")

    private val myIsMethodScope = property(false).provideDelegate(this, "kbenchIsMethodScope")

    private val myClass = string("").provideDelegate(this, "kbenchClass")
    private val myMethod = string("").provideDelegate(this, "kbenchMethod")

    private val myBenchmem = property(false).provideDelegate(this, "kbenchBenchmem")

    var isMethodScope: Boolean
        get() = myIsMethodScope.getValue(this)
        set(value) {
            myIsMethodScope.setValue(this, value)
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
        get() = myFilename.getValue(this) ?: ""
        set(value) {
            myFilename.setValue(this, value)
        }

    var benchType: KBenchType
        get() = KBenchType.from(myBenchType.getValue(this)!!)
        set(benchType) {
            myBenchType.setValue(this, benchType.command)
        }

    var countRuns: String
        get() = myCountRuns.getValue(this) ?: "5"
        set(value) {
            myCountRuns.setValue(this, value)
        }

    var benchmem: Boolean
        get() = myBenchmem.getValue(this)
        set(value) {
            myBenchmem.setValue(this, value)
        }
}
