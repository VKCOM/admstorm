package com.vk.admstorm.configuration.kbench

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.vk.admstorm.AdmService
import com.vk.admstorm.utils.KBenchUtils

open class KBenchConfigurationProducer : LazyRunConfigurationProducer<KBenchConfiguration>() {
    override fun getConfigurationFactory() =
        ConfigurationTypeUtil.findConfigurationType(KBenchConfigurationType::class.java)
            .configurationFactories[0]

    override fun isConfigurationFromContext(
        configuration: KBenchConfiguration,
        context: ConfigurationContext
    ): Boolean {
        if (!AdmService.getInstance(configuration.project).needBeEnabled()) return false

        val source = context.location?.psiElement ?: return false
        val element = source.parent ?: return false
        val filename = context.location?.virtualFile?.path ?: return false

        return when (element) {
            is PhpClass -> {
                configuration.filename == filename &&
                        configuration.className == element.name &&
                        !configuration.isMethodScope
            }
            is Method -> {
                val className = element.containingClass?.fqn ?: return false
                configuration.filename == filename &&
                        configuration.className == className &&
                        configuration.method == element.name &&
                        configuration.isMethodScope
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

        when (element) {
            is PhpClass -> {
                if (!KBenchUtils.isBenchmarkClass(element)) {
                    return false
                }

                val name = element.fqn

                configuration.className = name
                configuration.name = "KPHP Bench '${name.trimStart('\\')}'"
            }
            is Method -> {
                if (!KBenchUtils.isBenchmarkMethod(element)) {
                    return false
                }

                val className = element.containingClass?.fqn ?: return false

                configuration.className = className
                configuration.method = element.name
                configuration.name = "KPHP Bench '${element.fqn.replace(".", "::")}'"
                configuration.isMethodScope = true
            }
            else -> return false
        }

        return true
    }
}
