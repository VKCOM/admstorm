package com.vk.admstorm.configuration.phplinter

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

open class PhpLinterConfigurationProducer : LazyRunConfigurationProducer<PhpLinterConfiguration>() {
    override fun getConfigurationFactory() =
        ConfigurationTypeUtil.findConfigurationType(PhpLinterConfigurationType::class.java)
            .configurationFactories[0]

    override fun isConfigurationFromContext(
        configuration: PhpLinterConfiguration,
        context: ConfigurationContext
    ): Boolean = false

    override fun setupConfigurationFromContext(
        configuration: PhpLinterConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean = false
}
