package com.vk.admstorm.configuration.kbench

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.vk.admstorm.AdmService
import com.vk.admstorm.utils.KBenchUtils

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
        if (!AdmService.getInstance(configuration.project).needBeEnabled()) return false

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

        val source = context.location?.psiElement ?: return false
        val element = source.parent ?: return false
        val filename = context.location?.virtualFile?.path ?: return false

        if (configuration.benchType != benchType()) return false

        return when (element) {
            is PhpClass -> {
                configuration.filename == filename &&
                        configuration.className == element.fqn &&
                        configuration.scope == KBenchScope.Class
            }
            is Method -> {
                val className = element.containingClass?.fqn ?: return false
                configuration.filename == filename &&
                        configuration.className == className &&
                        configuration.methodName == element.name &&
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
        if (!AdmService.getInstance(configuration.project).needBeEnabled()) return false

        val source = sourceElement.get()
        val element = source.parent ?: return false

        val filename = context.location?.virtualFile?.path ?: return false
        configuration.filename = filename
        configuration.benchType = benchType()

        when (element) {
            is PhpClass -> {
                if (!KBenchUtils.isBenchmarkClass(element)) {
                    return false
                }

                val fullName = element.fqn
                val name = KBenchUtils.className(fullName)

                configuration.className = fullName
                configuration.name = "${namePrefix()} $name"
            }
            is Method -> {
                if (!KBenchUtils.isBenchmarkMethod(element)) {
                    return false
                }

                val className = element.containingClass?.fqn ?: return false
                val methodName = element.name
                val fullName = KBenchUtils.className(className) + "::" + KBenchUtils.benchmarkName(methodName)

                configuration.className = className
                configuration.methodName = methodName
                configuration.name = "${namePrefix()} $fullName"
                configuration.scope = KBenchScope.Method
            }
            else -> return false
        }

        return true
    }
}
