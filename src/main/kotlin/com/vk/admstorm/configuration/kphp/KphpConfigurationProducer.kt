package com.vk.admstorm.configuration.kphp

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

open class KphpConfigurationProducer : LazyRunConfigurationProducer<KphpConfiguration>() {
    override fun getConfigurationFactory() =
        ConfigurationTypeUtil.findConfigurationType(KphpConfigurationType::class.java).configurationFactories[0]

    override fun isConfigurationFromContext(configuration: KphpConfiguration, context: ConfigurationContext): Boolean {
        if (!configuration.project.pluginEnabled()) return false

        if (configuration.runType != KphpRunType.Sc) {
            return false
        }

        val el = context.location?.psiElement ?: return false
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

            if (configuration.name != "KPHP Script '$filename'") {
                return false
            }
            if (configuration.runType != KphpRunType.Sc) {
                return false
            }

            val relativeName = MyPathUtils.remotePhpFolderRelativePathByLocalPath(containingFile.project, filepath)
            if (configuration.parameters != relativeName) {
                return false
            }

            return true
        }

        return false
    }

    override fun setupConfigurationFromContext(
        configuration: KphpConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        if (!configuration.project.pluginEnabled()) return false

        val el = sourceElement.get()
        val parent = el.parent
        if (el is PhpClass ||
            parent is PhpClass || parent is MethodImpl || parent is FunctionImpl
        ) {
            return false
        }

        val containingFile = el.containingFile

        if (el is PhpFileImpl || containingFile is PhpFileImpl) {
            val filename = containingFile.name
            val filepath = containingFile.virtualFile.path

            configuration.name = "KPHP Script '$filename'"
            configuration.runType = KphpRunType.Sc

            val relativeName = MyPathUtils.remotePhpFolderRelativePathByLocalPath(containingFile.project, filepath)
            configuration.parameters = relativeName
            return true
        }

        return false
    }
}
