package com.vk.admstorm.configuration.phpunit

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.findParentOfType
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

    private fun isApiTest(element: PsiElement) = isSpecificTest(element, "/tests/api/")

    private fun isPackageTest(element: PsiElement) = isSpecificTest(element, "/packages/")

    private fun isSpecificTest(element: PsiElement, subPath: String): Boolean {
        val file = if (element is PsiDirectory)
            element.virtualFile
        else
            element.containingFile?.virtualFile

        if (file == null) return false
        return file.path.normalizeSlashes().contains(subPath)
    }

    override fun isConfigurationFromContext(conf: RemotePhpUnitConfiguration, context: ConfigurationContext): Boolean {
        if (!conf.project.pluginEnabled()) return false

        val el = context.location?.psiElement ?: return false
        if (el is PsiDirectory) {
            if (!el.virtualFile.path.contains("tests")) {
                return false
            }

            if (!conf.isDirectoryScope) {
                return false
            }

            return conf.directory == el.virtualFile.path
        }

        if (el is PhpFile) {
            if (!PhpUnitUtil.isPhpUnitTestFile(el)) {
                return false
            }

            if (conf.isDirectoryScope || conf.isMethodScope) {
                return false
            }

            val klass = PhpUnitUtil.findTestClass(el) ?: return false

            return conf.className == klass.fqn && conf.filename == el.virtualFile.path
        }

        if (el.parent is MethodImpl) {
            if (!conf.isMethodScope) {
                return false
            }

            val method = el.parent as MethodImpl
            val klass = PsiTreeUtil.findFirstParent(method) { parent ->
                parent is PhpClass
            } as PhpClass? ?: return false

            if (!PhpUnitUtil.isTestClass(klass)) {
                return false
            }

            return conf.className == klass.fqn && conf.method == method.name
        }

        if (el.parent !is PhpClass) {
            return false
        }

        val klass = el.parent as PhpClass
        val file = klass.containingFile

        if (!PhpUnitUtil.isTestClass(klass)) {
            return false
        }

        return conf.className == klass.fqn &&
                conf.filename == file.virtualFile.path &&
                conf.isClassScope
    }

    override fun setupConfigurationFromContext(
        conf: RemotePhpUnitConfiguration,
        context: ConfigurationContext,
        source: Ref<PsiElement>
    ): Boolean {
        val project = conf.project
        if (!project.pluginEnabled()) return false

        val projectDir = MyPathUtils.resolveProjectDir(project)
        val filepath = context.location?.virtualFile?.path ?: return false
        val element = source.get() ?: return false

        if (isApiTest(element)) {
            val suffix = " API Test"

            conf.isApiTest = true
            conf.phpUnitPath = "$projectDir/tests/api/phpunit.xml"
            conf.configPath = "$projectDir/vendor/bin/phpunit"

            return setupCommonTest(conf, element, filepath, suffix)
        }

        if (isPackageTest(element)) {
            val packageRoot = PackagesTestUtils.rootFolder(filepath)
            val packageName = PackagesTestUtils.packageName(project, packageRoot)
            val suffix = " '$packageName' Package Test"

            conf.isPackageTest = true
            conf.phpUnitPath = (packageRoot ?: projectDir) + "/vendor/bin/phpunit"
            conf.configPath = (packageRoot ?: projectDir) + "/phpunit.xml"

            return setupCommonTest(conf, element, filepath, suffix, replaceDirectoryName = true)
        }

        conf.phpUnitPath = "$projectDir/vendor/bin/phpunit"
        conf.configPath = "$projectDir/phpunit.xml"

        return setupCommonTest(conf, element, filepath)
    }

    private fun setupCommonTest(
        conf: RemotePhpUnitConfiguration,
        element: PsiElement,
        filepath: String,
        suffixName: String = "",
        replaceDirectoryName: Boolean = false,
    ): Boolean {
        if (element is PsiDirectory) {
            if (!element.virtualFile.path.contains("tests")) {
                return false
            }

            val lastDir = File(filepath).name
            val configName = if (replaceDirectoryName) "Remote$suffixName" else "Remote '$lastDir'$suffixName"

            conf.name = configName
            conf.isDirectoryScope = true
            conf.directory = filepath

            return true
        }

        if (element is PhpFile) {
            if (!PhpUnitUtil.isPhpUnitTestFile(element)) {
                return false
            }

            val klass = PhpUnitUtil.findTestClass(element) ?: return false

            val className = klass.name
            val configName = "Remote $className$suffixName"

            conf.name = configName
            conf.className = klass.fqn
            conf.filename = filepath
            conf.isDirectoryScope = false
            conf.isClassScope = true

            return true
        }

        if (element.parent is PhpClass) {
            val klass = element.parent as PhpClass

            if (!PhpUnitUtil.isTestClass(klass)) {
                return false
            }

            val className = klass.name
            val configName = "Remote $className$suffixName"

            conf.name = configName
            conf.className = klass.fqn
            conf.filename = filepath
            conf.isDirectoryScope = false
            conf.isClassScope = true

            return true
        }

        if (element.parent is MethodImpl) {
            val method = element.parent as MethodImpl
            val klass = method.findParentOfType<PhpClass>() ?: return false

            if (!PhpUnitUtil.isTestClass(klass)) {
                return false
            }

            val className = klass.name
            val configName = "Remote $className::${method.name}$suffixName"

            conf.name = configName
            conf.className = klass.fqn
            conf.method = method.name
            conf.filename = filepath
            conf.isDirectoryScope = false
            conf.isMethodScope = true

            return true
        }

        return false
    }
}
