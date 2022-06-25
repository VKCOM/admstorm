package com.vk.admstorm.configuration.kbench

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.vk.admstorm.utils.extensions.pluginEnabled

abstract class KBenchBaseConfigurationProducer : LazyRunConfigurationProducer<KBenchConfiguration>() {
    abstract fun configurationId(): String
    open fun benchType(): KBenchType = KBenchType.Bench
    open fun namePrefix(): String = "KPHP"

    override fun getConfigurationFactory(): ConfigurationFactory =
        ConfigurationTypeUtil.findConfigurationType(configurationId())!!
            .configurationFactories[0]

    override fun isConfigurationFromContext(
        configuration: KBenchConfiguration,
        context: ConfigurationContext
    ): Boolean {
        if (!configuration.project.pluginEnabled()) return false

        // Так как при конфигурации сравнения мы хотим каждый раз, запуская,
        // через иконку рядом с методом или классом получать окно в котором
        // выбираем классы и методы, то все уже созданные конфигурации не
        // должны совпадать с новой.
        //
        // Однако запуск через конфигурации запуска все еще будет работать
        // и использовать уже ранее введенное имя.
        if (configuration.benchType == KBenchType.BenchAb) {
            return false
        }

        val element = context.location?.psiElement ?: return false
        val parent = element.parent ?: return false
        val filename = context.location?.virtualFile?.path ?: return false

        if (configuration.benchType != benchType()) return false

        if (element is PhpFile) {
            val klass = KBenchUtils.findBenchmarkClass(element) ?: return false

            return configuration.filename == filename &&
                    configuration.className == klass.fqn &&
                    configuration.scope == KBenchScope.Class
        }

        return when (parent) {
            is PhpClass -> {
                configuration.filename == filename &&
                        configuration.className == parent.fqn &&
                        configuration.scope == KBenchScope.Class
            }
            is Method -> {
                val className = parent.containingClass?.fqn ?: return false
                configuration.filename == filename &&
                        configuration.className == className &&
                        configuration.methodName == parent.name &&
                        configuration.scope == KBenchScope.Method
            }
            else -> false
        }
    }

    override fun setupConfigurationFromContext(
        configuration: KBenchConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        if (!configuration.project.pluginEnabled()) return false

        val element = sourceElement.get()
        val parent = element.parent ?: return false

        val filename = context.location?.virtualFile?.path ?: return false
        configuration.filename = filename
        configuration.benchType = benchType()

        if (element is PhpFile) {
            if (!KBenchUtils.isBenchmarkFile(element)) {
                return false
            }

            val klass = KBenchUtils.findBenchmarkClass(element) ?: return false

            val fqn = klass.fqn
            val name = klass.name

            configuration.className = fqn
            configuration.name = "${namePrefix()} $name"

            return true
        }

        when (parent) {
            is PhpClass -> {
                if (!KBenchUtils.isBenchmarkClass(parent)) {
                    return false
                }

                val fqn = parent.fqn
                val name = parent.name

                configuration.className = fqn
                configuration.name = "${namePrefix()} $name"
            }
            is Method -> {
                if (!KBenchUtils.isBenchmarkMethod(parent)) {
                    return false
                }

                val className = parent.containingClass?.fqn ?: return false
                val methodName = parent.name
                val fqn = KBenchUtils.className(className) + "::" + KBenchUtils.benchmarkName(methodName)

                configuration.className = className
                configuration.methodName = methodName
                configuration.name = "${namePrefix()} $fqn"
                configuration.scope = KBenchScope.Method
            }
            else -> return false
        }

        return true
    }

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext) =
        other.configuration !is KBenchConfiguration
}
