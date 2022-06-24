package com.vk.admstorm.executors

import com.intellij.execution.Output
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.vk.admstorm.configuration.problems.panels.ProblemsPanel
import com.vk.admstorm.console.Console
import com.vk.admstorm.executors.tabs.ProblemsTab
import com.vk.admstorm.parsers.KphpErrorsParser
import com.vk.admstorm.parsers.PhpLinterWarningsParser
import com.vk.admstorm.utils.ServerNameProvider
import java.util.function.BiConsumer
import javax.swing.Icon

class PushToRemoteExecutor(project: Project, command: String) :
    BaseRunnableExecutor(
        Config(tabName = "Push from ${ServerNameProvider.name()} to Gitlab", command = command),
        project
    ) {

    private var myOutputHandler: BiConsumer<Output, Console> = BiConsumer { _, _ -> }

    fun withOutputHandler(handler: BiConsumer<Output, Console>) {
        myOutputHandler = handler
    }

    override fun onFinish() {
        val output = output()

        invokeLater {
            myOutputHandler.accept(output, Console(project))
        }

        invokeLater {
            if (output.exitCode != 0) {
                val problems = when {
                    output.stderr.contains("<critical>") -> {
                        PhpLinterWarningsParser.parse(project, output.stderr)
                    }
                    output.stdout.contains("Compilation error") -> {
                        KphpErrorsParser.parse(project, output.stdout)
                    }
                    else -> emptyList()
                }

                if (problems.isNotEmpty()) {
                    val problemsTab = ProblemsTab()
                    addTab(problemsTab)

                    invokeLater {
                        selectTab(problemsTab)
                    }

                    val panel = ProblemsPanel(project, problems)
                    problemsTab.panel.addToCenter(panel)
                }
            }
        }
    }

    override fun icon(): Icon = AllIcons.Vcs.Push
}
