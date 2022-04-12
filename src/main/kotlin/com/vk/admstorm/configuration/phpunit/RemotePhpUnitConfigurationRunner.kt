package com.vk.admstorm.configuration.phpunit

import com.intellij.execution.ExecutionManager
import com.intellij.execution.ExecutionResult
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ExecutionUiService
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.vk.admstorm.configuration.WithSshConfigurationRunner
import org.jetbrains.concurrency.resolvedPromise

class RemotePhpUnitConfigurationRunner : WithSshConfigurationRunner(
    withDebug = true,
    inEDT = true,
    RemotePhpUnitConfiguration::class
) {
    override fun getRunnerId() = "RemotePhpUnit"

    override fun run(environment: ExecutionEnvironment) {
        val state = environment.state ?: return
        ExecutionManager.getInstance(environment.project).startRunProfile(environment) {
            FileDocumentManager.getInstance().saveAllDocuments()
            resolvedPromise(showRunContent(state.execute(environment.executor, this), environment))
        }
    }

    private fun showRunContent(
        executionResult: ExecutionResult?,
        environment: ExecutionEnvironment
    ): RunContentDescriptor? {
        return executionResult?.let {
            ExecutionUiService.getInstance().showRunContent(it, environment)
        }
    }
}
