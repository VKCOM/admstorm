package com.vk.admstorm.configuration.phpunit

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.impl.MethodImpl
import com.jetbrains.php.phpunit.PhpUnitUtil
import com.vk.admstorm.AdmService
import java.io.File

open class RemotePhpUnitConfigurationProducer :
    LazyRunConfigurationProducer<RemotePhpUnitConfiguration>() {

    override fun getConfigurationFactory() =
        ConfigurationTypeUtil.findConfigurationType(RemotePhpUnitConfigurationType::class.java).configurationFactories[0]

    override fun isConfigurationFromContext(
        configuration: RemotePhpUnitConfiguration,
        context: ConfigurationContext
    ): Boolean {
        if (!AdmService.getInstance(configuration.project).needBeEnabled()) return false

        val el = context.location?.psiElement ?: return false
        if (el is PsiDirectory) {
            if (!el.virtualFile.path.contains("tests")) {
                return false
            }

            if (!configuration.isDirectoryScope) {
                return false
            }

            return configuration.directory == el.virtualFile.path
        }

        if (el is PhpFile) {
            if (!PhpUnitUtil.isPhpUnitTestFile(el)) {
                return false
            }

            if (configuration.isDirectoryScope || configuration.isMethodScope) {
                return false
            }

            val klass = PhpUnitUtil.findTestClass(el) ?: return false

            val className = klass.name
            val configName = "Remote $className"

            return configuration.className == configName && configuration.filename == el.virtualFile.path
        }

        if (el.parent !is PhpClass) {
            return false
        }

        val klass = el.parent as PhpClass
        val file = klass.containingFile

        if (!PhpUnitUtil.isTestClass(klass)) {
            return false
        }

        val className = klass.name
        val configName = "Remote $className"

        return configuration.className == configName && configuration.filename == file.virtualFile.path
    }

    override fun setupConfigurationFromContext(
        configuration: RemotePhpUnitConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        if (!AdmService.getInstance(configuration.project).needBeEnabled()) return false

        val filepath = context.location?.virtualFile?.path ?: return false

        val el = sourceElement.get()

        if (el is PsiDirectory) {
            if (!el.virtualFile.path.contains("tests")) {
                return false
            }

            val lastDir = File(filepath).name
            val configName = "Remote $lastDir"

            configuration.name = configName
            configuration.isDirectoryScope = true
            configuration.directory = filepath

            return true
        }

        if (el is PhpFile) {
            if (!PhpUnitUtil.isPhpUnitTestFile(el)) {
                return false
            }

            val klass = PhpUnitUtil.findTestClass(el) ?: return false

            val className = klass.name
            val configName = "Remote $className"

            configuration.name = configName
            configuration.className = klass.fqn
            configuration.filename = filepath
            configuration.isDirectoryScope = false
            configuration.isClassScope = true

            return true
        }

        if (el.parent is PhpClass) {
            val klass = el.parent as PhpClass

            if (!PhpUnitUtil.isTestClass(klass)) {
                return false
            }

            val className = klass.name
            val configName = "Remote $className"

            configuration.name = configName
            configuration.className = klass.fqn
            configuration.filename = filepath
            configuration.isDirectoryScope = false
            configuration.isClassScope = true
            return true
        }

        if (el.parent is MethodImpl) {
            val method = el.parent as MethodImpl
            val klass = PsiTreeUtil.findFirstParent(method) { parent ->
                parent is PhpClass
            } as PhpClass? ?: return false

            val className = klass.name
            val configName = "Remote $className::${method.name}"

            configuration.name = configName
            configuration.className = klass.fqn
            configuration.method = method.name
            configuration.filename = filepath
            configuration.isDirectoryScope = false
            configuration.isMethodScope = true
            return true
        }

        return false
    }
}
