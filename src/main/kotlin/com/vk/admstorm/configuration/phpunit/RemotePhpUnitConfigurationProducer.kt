package com.vk.admstorm.configuration.phpunit

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
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
            val rawPath = el.virtualFile.path
            // if run tests for package folder
            val nextTestsFolder = File(rawPath, "tests")
            val path = if (nextTestsFolder.exists()) {
                nextTestsFolder.path
            } else {
                rawPath
            }

            if (!path.contains("tests")) {
                return false
            }

            if (conf.scope != PhpUnitScope.Directory) {
                return false
            }

            return conf.directory == path
        }

        if (el is PhpFile) {
            if (!PhpUnitUtil.isPhpUnitTestFile(el)) {
                return false
            }

            if (conf.scope == PhpUnitScope.Directory || conf.scope == PhpUnitScope.Method) {
                return false
            }

            val klass = PhpUnitUtil.findTestClass(el) ?: return false

            return conf.className == klass.fqn && conf.filename == el.virtualFile.path
        }

        if (el.parent is MethodImpl) {
            if (conf.scope != PhpUnitScope.Method) {
                return false
            }

            val method = el.parent as MethodImpl
            val klass = method.findParentOfType<PhpClass>() ?: return false

            if (!PhpUnitUtil.isTestClass(klass)) {
                return false
            }

            return conf.className == klass.fqn && conf.methodName == method.name
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
                conf.scope == PhpUnitScope.Class
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
            conf.phpUnitExe = "$projectDir/tests/api/phpunit.xml"
            conf.phpUnitConfig = "$projectDir/vendor/bin/phpunit"

            return setupCommonTest(conf, element, filepath, suffix)
        }

        if (isPackageTest(element)) {
            val packageRoot = PackagesTestUtils.rootFolder(filepath)
            val packageName = PackagesTestUtils.packageName(project, packageRoot)
            val suffix = " '$packageName' Package Test"

            conf.isPackageTest = true
            conf.phpUnitExe = (packageRoot ?: projectDir) + "/vendor/bin/phpunit"
            conf.phpUnitConfig = (packageRoot ?: projectDir) + "/phpunit.xml"

            return setupCommonTest(conf, element, filepath, suffix, replaceDirectoryName = true)
        }

        conf.phpUnitExe = "$projectDir/vendor/bin/phpunit"
        conf.phpUnitConfig = "$projectDir/phpunit.xml"

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
            // if run tests for package folder
            val nextTestsFolder = File(filepath, "tests")
            val path = if (nextTestsFolder.exists()) {
                nextTestsFolder.path
            } else {
                filepath
            }

            if (!path.contains("tests")) {
                return false
            }

            val lastDir = File(filepath).name
            val configName = if (replaceDirectoryName) "Remote$suffixName" else "Remote '$lastDir'$suffixName"

            conf.name = configName
            conf.scope = PhpUnitScope.Directory
            conf.directory = path

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
            conf.scope = PhpUnitScope.Class

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
            conf.scope = PhpUnitScope.Class

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
            conf.methodName = method.name
            conf.filename = filepath
            conf.scope = PhpUnitScope.Method

            return true
        }

        return false
    }
}
