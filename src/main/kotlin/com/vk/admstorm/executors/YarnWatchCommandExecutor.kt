package com.vk.admstorm.executors

import com.intellij.execution.OutputListener
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.vk.admstorm.git.sync.files.RemoteFileManager
import com.vk.admstorm.services.YarnWatchService
import com.vk.admstorm.ui.MyIcons
import javax.swing.Icon

class YarnWatchCommandExecutor(project: Project, command: String) :
    BaseRunnableExecutor(Config(tabName = "watch", layoutName = "Yarn watch", command = command), project) {

    companion object {
        private val LOG = logger<YarnWatchCommandExecutor>()
    }

    private val service
        get() = YarnWatchService.getInstance(project)

    override fun onReady() {
        service.setWorking(false)
    }

    override fun onStop() {
        service.setWorking(false)
    }

    override fun onStopBeforeRerun() {
        service.setWorking(false)
    }

    override fun onRerun() {
        service.setWorking(true)
    }

    override fun onToolWindowShow() {
        service.clearErrorsState()
    }

    override fun listeners(): List<ProcessAdapter> {
        return listOf(
            object : OutputListener() {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    val text = event.text
                    if (needUpdateAutogeneratedFiles(text)) {
                        LOG.info("Need update autogenerated files")
                        RemoteFileManager(project).doUpdateAutogeneratedFiles(false)
                    }

                    if (isErrorLine(text)) {
                        LOG.info("Line $text looks like error")
                        service.setErrorsState()
                    }
                }
            }
        )
    }

    private fun needUpdateAutogeneratedFiles(text: String): Boolean {
        return text.contains("webpack") && text.contains("compiled successfully") ||
                text.contains("Elephize recompilation done")
    }

    private fun isErrorLine(text: String): Boolean {
        val line = text.trim()
        return line.startsWith("ERROR") ||
                line.startsWith("Error:") ||
                line.startsWith("[E ")
    }

    override fun icon(): Icon = MyIcons.yarn

    override fun executorInstance() = YarnWatchExecutor.getRunExecutorInstance()
    override fun executorToolWindowId() = YarnWatchExecutor.TOOL_WINDOW_ID
    override fun runnerTitle() = "Yarn Watch"
    override fun runnerId() = "YarnWatch"
}
