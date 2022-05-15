package com.vk.admstorm.configuration.kbench

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.openapi.project.Project
import com.vk.admstorm.ui.MyIcons

class KBenchKphpConfigurationType : ConfigurationTypeBase(
    ID, "KPHP Benchmark",
    "KPHP Benchmark",
    MyIcons.kphpBench
) {
    companion object {
        const val ID = "KBenchKphpConfiguration"
    }

    init {
        addFactory(object : ConfigurationFactory(this) {
            override fun getId() = ID

            override fun createTemplateConfiguration(project: Project) =
                KBenchConfiguration(project, this, "Run KPHP Benchmark")

            override fun getOptionsClass() = KBenchConfigurationOptions::class.java
        })
    }
}

class KBenchPhpConfigurationType : ConfigurationTypeBase(
    ID, "PHP Benchmark",
    "PHP Benchmark configuration type",
    MyIcons.kphpBench
) {
    companion object {
        const val ID = "KBenchPhpConfiguration"
    }

    init {
        addFactory(object : ConfigurationFactory(this) {
            override fun getId() = ID

            override fun createTemplateConfiguration(project: Project) =
                KBenchConfiguration(project, this, "Run PHP Benchmark")

            override fun getOptionsClass() = KBenchConfigurationOptions::class.java
        })
    }
}

class KBenchKphpVsPhpConfigurationType : ConfigurationTypeBase(
    ID, "KPHP vs PHP Benchmark",
    "KPHP vs PHP Benchmark configuration type",
    MyIcons.kphpBench
) {
    companion object {
        const val ID = "KBenchKphpVsPhpConfiguration"
    }

    init {
        addFactory(object : ConfigurationFactory(this) {
            override fun getId() = ID

            override fun createTemplateConfiguration(project: Project) =
                KBenchConfiguration(project, this, "Run KPHP vs PHP Benchmark")

            override fun getOptionsClass() = KBenchConfigurationOptions::class.java
        })
    }
}

class KBenchKphpAbConfigurationType : ConfigurationTypeBase(
    ID, "Compare with other",
    "KPHP AB Benchmark configuration type",
    MyIcons.kphpBench
) {
    companion object {
        const val ID = "KBenchKphpAbConfiguration"
    }

    init {
        addFactory(object : ConfigurationFactory(this) {
            override fun getId() = ID

            override fun createTemplateConfiguration(project: Project) =
                KBenchConfiguration(project, this, "Run KPHP AB Benchmark")

            override fun getOptionsClass() = KBenchConfigurationOptions::class.java
        })
    }
}
