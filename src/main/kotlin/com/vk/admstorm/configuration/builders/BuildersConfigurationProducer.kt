package com.vk.admstorm.configuration.builders

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

open class BuildersConfigurationProducer : LazyRunConfigurationProducer<BuildersConfiguration>() {
    override fun getConfigurationFactory() =
        ConfigurationTypeUtil.findConfigurationType(BuildersConfigurationType::class.java)
            .configurationFactories[0]

    override fun isConfigurationFromContext(
        configuration: BuildersConfiguration,
        context: ConfigurationContext
    ): Boolean = false

    override fun setupConfigurationFromContext(
        configuration: BuildersConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean = false
}
