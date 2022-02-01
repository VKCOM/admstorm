package com.vk.admstorm.configuration.kbench

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.impl.PhpClassImpl
import com.vk.admstorm.AdmService

open class KBenchConfigurationProducer : LazyRunConfigurationProducer<KBenchConfiguration>() {
    override fun getConfigurationFactory() =
        ConfigurationTypeUtil.findConfigurationType(KBenchConfigurationType::class.java)
            .configurationFactories[0]

    private fun isBenchmarkClass(klass: PhpClassImpl) = klass.name.startsWith("Benchmark")

    override fun isConfigurationFromContext(
        configuration: KBenchConfiguration,
        context: ConfigurationContext
    ): Boolean {
        if (!AdmService.getInstance(configuration.project).needBeEnabled()) return false

        val el = context.location?.psiElement ?: return false
        if (el !is PhpClassImpl) {
            return false
        }

        if (!isBenchmarkClass(el)) {
            return false
        }

        val filename = context.location?.virtualFile?.path ?: return false
        return configuration.scriptName == filename
    }

    override fun setupConfigurationFromContext(
        configuration: KBenchConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        if (!AdmService.getInstance(configuration.project).needBeEnabled()) return false

        val el = sourceElement.get()
        if (el !is PhpClassImpl) {
            return false
        }

        if (!isBenchmarkClass(el)) {
            return false
        }

        val filename = context.location?.virtualFile?.path ?: return false

        configuration.name = "KPHP Bench '" + el.fqn.trimStart('\\') + "'"
        configuration.scriptName = filename
        return true
    }
}
