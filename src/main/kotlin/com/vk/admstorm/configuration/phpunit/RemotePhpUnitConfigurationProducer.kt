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
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.extensions.normalizeSlashes
import com.vk.admstorm.utils.extensions.pluginEnabled
import java.io.File

open class RemotePhpUnitConfigurationProducer :
    LazyRunConfigurationProducer<RemotePhpUnitConfiguration>() {

    override fun getConfigurationFactory() =
        ConfigurationTypeUtil.findConfigurationType(RemotePhpUnitConfigurationType::class.java).configurationFactories[0]

    private fun isApiTest(element: PsiElement): Boolean {
        val file = if (element is PsiDirectory)
            element.virtualFile
        else
            element.containingFile?.virtualFile

        if (file == null) return false
        return file.path.normalizeSlashes().contains("tests/api")
    }

    override fun isConfigurationFromContext(
        configuration: RemotePhpUnitConfiguration,
        context: ConfigurationContext
    ): Boolean {
        if (!configuration.project.pluginEnabled()) return false

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

            return configuration.className == klass.fqn && configuration.filename == el.virtualFile.path
        }

        if (el.parent is MethodImpl) {
            if (!configuration.isMethodScope) {
                return false
            }

            val method = el.parent as MethodImpl
            val klass = PsiTreeUtil.findFirstParent(method) { parent ->
                parent is PhpClass
            } as PhpClass? ?: return false

            if (!PhpUnitUtil.isTestClass(klass)) {
                return false
            }

            return configuration.className == klass.fqn && configuration.method == method.name
        }

        if (el.parent !is PhpClass) {
            return false
        }

        val klass = el.parent as PhpClass
        val file = klass.containingFile

        if (!PhpUnitUtil.isTestClass(klass)) {
            return false
        }

        return configuration.className == klass.fqn &&
                configuration.filename == file.virtualFile.path &&
                configuration.isClassScope
    }

    override fun setupConfigurationFromContext(
        configuration: RemotePhpUnitConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        if (!configuration.project.pluginEnabled()) return false

        val filepath = context.location?.virtualFile?.path ?: return false
        val element = sourceElement.get()

        val isApi = isApiTest(element)
        val suffixName = if (isApi) " API Test" else ""
        configuration.isApiTest = isApi

        val configPath = MyPathUtils.resolveProjectDir(configuration.project) + if (isApi)
            "/tests/api/phpunit.xml"
        else
            "/phpunit.xml"

        configuration.configPath = configPath

        if (element is PsiDirectory) {
            if (!element.virtualFile.path.contains("tests")) {
                return false
            }

            val lastDir = File(filepath).name
            val configName = "Remote '$lastDir'$suffixName"

            configuration.name = configName
            configuration.isDirectoryScope = true
            configuration.directory = filepath

            return true
        }

        if (element is PhpFile) {
            if (!PhpUnitUtil.isPhpUnitTestFile(element)) {
                return false
            }

            val klass = PhpUnitUtil.findTestClass(element) ?: return false

            val className = klass.name
            val configName = "Remote $className$suffixName"

            configuration.name = configName
            configuration.className = klass.fqn
            configuration.filename = filepath
            configuration.isDirectoryScope = false
            configuration.isClassScope = true

            return true
        }

        if (element.parent is PhpClass) {
            val klass = element.parent as PhpClass

            if (!PhpUnitUtil.isTestClass(klass)) {
                return false
            }

            val className = klass.name
            val configName = "Remote $className$suffixName"

            configuration.name = configName
            configuration.className = klass.fqn
            configuration.filename = filepath
            configuration.isDirectoryScope = false
            configuration.isClassScope = true
            return true
        }

        if (element.parent is MethodImpl) {
            val method = element.parent as MethodImpl
            val klass = PsiTreeUtil.findFirstParent(method) { parent ->
                parent is PhpClass
            } as PhpClass? ?: return false

            if (!PhpUnitUtil.isTestClass(klass)) {
                return false
            }

            val className = klass.name
            val configName = "Remote $className::${method.name}$suffixName"

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
