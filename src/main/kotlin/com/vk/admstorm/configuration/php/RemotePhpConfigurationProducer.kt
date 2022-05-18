package com.vk.admstorm.configuration.php

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.PhpFileImpl
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.impl.FunctionImpl
import com.jetbrains.php.lang.psi.elements.impl.MethodImpl
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.extensions.pluginEnabled

open class RemotePhpConfigurationProducer : LazyRunConfigurationProducer<RemotePhpConfiguration>() {
    override fun getConfigurationFactory() =
        ConfigurationTypeUtil.findConfigurationType(RemotePhpConfigurationType::class.java)
            .configurationFactories[0]

    override fun isConfigurationFromContext(
        configuration: RemotePhpConfiguration,
        context: ConfigurationContext
    ): Boolean {
        if (!configuration.project.pluginEnabled()) return false

        val el = context.location?.psiElement ?: return false

        // if element for phpunit run icon
        val parent = el.parent
        if (el is PhpClass || parent is PhpClass ||
            parent is MethodImpl || parent is FunctionImpl
        ) {
            return false
        }

        val containingFile = el.containingFile

        if (el is PhpFileImpl || containingFile is PhpFileImpl) {
            val filename = containingFile.name
            val filepath = containingFile.virtualFile.path

            if (configuration.name != "Remote PHP '$filename'") {
                return false
            }

            val relativeName = MyPathUtils.remotePhpFolderRelativePathByLocalPath(containingFile.project, filepath)
            if (configuration.scriptName != relativeName) {
                return false
            }

            return true
        }

        return false
    }

    override fun setupConfigurationFromContext(
        configuration: RemotePhpConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        if (!configuration.project.pluginEnabled()) return false

        val el = sourceElement.get()

        // if element for phpunit run icon
        val parent = el.parent
        if (el is PhpClass || parent is PhpClass ||
            parent is MethodImpl || parent is FunctionImpl
        ) {
            return false
        }

        val containingFile = el.containingFile

        if (el is PhpFileImpl || containingFile is PhpFileImpl) {
            val filename = containingFile.name
            val filepath = containingFile.virtualFile.path

            configuration.name = "Remote PHP '$filename'"

            val relativeName = MyPathUtils.remotePhpFolderRelativePathByLocalPath(containingFile.project, filepath)
            configuration.scriptName = relativeName
            return true
        }

        return false
    }
}
