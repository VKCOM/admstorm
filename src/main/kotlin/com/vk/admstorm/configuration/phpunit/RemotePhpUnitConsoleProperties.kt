package com.vk.admstorm.configuration.phpunit

import com.intellij.execution.Executor
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.ui.ConsoleView
import com.jetbrains.php.phpunit.PhpUnitQualifiedNameLocationProvider
import com.jetbrains.php.util.pathmapper.PhpPathMapper
import com.vk.admstorm.utils.MyPathUtils

class RemotePhpUnitConsoleProperties(config: RemotePhpUnitConfiguration, executor: Executor) :
    SMTRunnerConsoleProperties(config, "RemotePhpUnit", executor),
    SMCustomMessagesParsing {

    internal class RemotePhpUnitEventsConverter(consoleProperties: TestConsoleProperties) :
        OutputToGeneralTestEventsConverter("RemotePhpUnit", consoleProperties)

    override fun createTestEventsConverter(
        testFrameworkName: String,
        consoleProperties: TestConsoleProperties
    ): OutputToGeneralTestEventsConverter {
        return RemotePhpUnitEventsConverter(consoleProperties)
    }

    private val provider = PhpUnitQualifiedNameLocationProvider.create(
        PhpPathMapper.createDefaultMapper()
    )

    override fun createRerunFailedTestsAction(consoleView: ConsoleView?) =
        RemotePhpUnitRerunFailedTestsAction(consoleView!!, this)

    /**
     * @see com.vk.admstorm.configuration.kbench.KBenchConsoleProperties.getTestLocator
     */
    override fun getTestLocator() = SMTestLocator { protocol, path, project, scope ->
        val localPath = MyPathUtils.absoluteLocalPathByAbsoluteRemotePath(project, path)
            ?: return@SMTestLocator emptyList()
        return@SMTestLocator provider.getLocation(protocol, localPath, project, scope)
    }
}
