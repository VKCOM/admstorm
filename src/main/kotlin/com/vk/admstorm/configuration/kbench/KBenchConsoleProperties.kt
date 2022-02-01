package com.vk.admstorm.configuration.kbench

import com.intellij.execution.Executor
import com.intellij.execution.PsiLocation
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.psi.PsiManager
import com.jetbrains.php.lang.psi.PhpFileImpl
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MyUtils

class KBenchConsoleProperties(config: KBenchConfiguration, executor: Executor) :
    SMTRunnerConsoleProperties(config, "KBench", executor),
    SMCustomMessagesParsing {

    internal class KBenchEventsConverter(consoleProperties: TestConsoleProperties) :
        OutputToGeneralTestEventsConverter("KBench", consoleProperties)

    override fun createTestEventsConverter(
        testFrameworkName: String,
        consoleProperties: TestConsoleProperties
    ): OutputToGeneralTestEventsConverter {
        return KBenchEventsConverter(consoleProperties)
    }

    /**
     * A handler that runs when a specific test is clicked in the test
     * manager when there are service messages with locationHint.
     *
     * For example, your tool's output is as follows:
     *
     *   ##teamcity[testStarted name='Name' locationHint='php_qn:///absolute/path/file.php::ClassName::methodName']
     *   Some tool text output
     *   ##teamcity[testFinished name='Name']
     *
     * Everything from locationHint will be passed into this handler.
     * - `protocol` = "php_qn://"
     * - `path`     = "/absolute/path/file.php::ClassName::methodName"
     *
     * @return an array of Location
     *
     * @see [Service Messages](https://www.jetbrains.com/help/teamcity/service-messages.html)
     */
    override fun getTestLocator() = SMTestLocator { protocol, path, project, _ ->
        if (protocol != "php_qn") {
            return@SMTestLocator emptyList()
        }

        val parts = path.split("::")
        if (parts.size < 2) {
            return@SMTestLocator emptyList()
        }

        val remoteFilepath = parts[0]
        val className = parts[1]
        val methodName = if (parts.size == 3) parts[2] else null

        val localFilepath = MyPathUtils.absoluteLocalPathByAbsoluteRemotePath(project, remoteFilepath)
            ?: return@SMTestLocator emptyList()

        val virtualFile = MyUtils.virtualFileByName(localFilepath) ?: return@SMTestLocator emptyList()
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@SMTestLocator emptyList()
        if (psiFile !is PhpFileImpl) {
            return@SMTestLocator emptyList()
        }

        val klass = PhpPsiUtil.findClass(psiFile) { klass ->
            klass.fqn == className
        } ?: return@SMTestLocator emptyList()

        if (methodName == null) {
            return@SMTestLocator listOf(PsiLocation(klass))
        }

        val method = klass.methods.find { method ->
            method.name == methodName
        } ?: return@SMTestLocator emptyList()

        return@SMTestLocator listOf(PsiLocation(method))
    }
}
