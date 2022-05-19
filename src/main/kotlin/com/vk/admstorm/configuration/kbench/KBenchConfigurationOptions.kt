package com.vk.admstorm.configuration.kbench

import com.intellij.execution.configurations.RunConfigurationOptions

class KBenchConfigurationOptions : RunConfigurationOptions() {
    private val myScope = string("").provideDelegate(this, "kbenchScope")
    private val myBenchType = string("bench").provideDelegate(this, "kbenchBenchType")

    private val myCountIteration = property(5).provideDelegate(this, "kbenchCountIterations")

    private val myClass = string("").provideDelegate(this, "kbenchClass")
    private val myMethod = string("").provideDelegate(this, "kbenchMethod")
    private val myFilename = string("").provideDelegate(this, "kbenchFilename")
    private val myCompareClass = string("").provideDelegate(this, "kbenchCompareClass")
    private val myCompareMethod = string("").provideDelegate(this, "kbenchCompareMethod")

    private val myBenchmarkMemory = property(false).provideDelegate(this, "kbenchBenchmarkMemory")

    var scope: KBenchScope
        get() = KBenchScope.from(myScope.getValue(this) ?: "")
        set(value) {
            myScope.setValue(this, value.name)
        }

    var className: String
        get() = myClass.getValue(this) ?: ""
        set(value) {
            myClass.setValue(this, value)
        }

    var methodName: String
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

    var countIteration: Int
        get() = myCountIteration.getValue(this)
        set(value) {
            myCountIteration.setValue(this, value)
        }

    var benchmarkMemory: Boolean
        get() = myBenchmarkMemory.getValue(this)
        set(value) {
            myBenchmarkMemory.setValue(this, value)
        }

    var compareClassName: String
        get() = myCompareClass.getValue(this) ?: ""
        set(value) {
            myCompareClass.setValue(this, value)
        }

    var compareMethodName: String
        get() = myCompareMethod.getValue(this) ?: ""
        set(value) {
            myCompareMethod.setValue(this, value)
        }
}
