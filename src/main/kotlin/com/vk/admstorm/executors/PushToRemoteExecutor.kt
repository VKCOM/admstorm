package com.vk.admstorm.executors

import com.intellij.execution.Output
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.vk.admstorm.configuration.problems.panels.ProblemsPanel
import com.vk.admstorm.console.Console
import com.vk.admstorm.env.Env
import com.vk.admstorm.executors.tabs.ProblemsTab
import com.vk.admstorm.parsers.KphpErrorsParser
import com.vk.admstorm.parsers.PhpLinterWarningsParser
import java.util.function.BiConsumer
import javax.swing.Icon

class PushToRemoteExecutor(project: Project, command: String) :
    BaseRunnableExecutor(Config(name = "Push from ${Env.data.serverName} to Gitlab", command = command), project) {

    private var myOutputHandler: BiConsumer<Output, Console> = BiConsumer { _, _ -> }

    fun withOutputHandler(handler: BiConsumer<Output, Console>) {
        myOutputHandler = handler
    }

    override fun onReady() {
        val output = myOutputListener!!.output

        ApplicationManager.getApplication().invokeLater {
            myOutputHandler.accept(output, Console(myProject))
        }

        ApplicationManager.getApplication().invokeLater {
            if (output.exitCode != 0) {
                val problems = when {
                    output.stderr.contains("<critical>") -> {
                        PhpLinterWarningsParser.parse(myProject, output.stderr)
                    }
                    output.stdout.contains("Compilation error") -> {
                        KphpErrorsParser.parse(myProject, output.stdout)
                    }
                    else -> emptyList()
                }

                if (problems.isNotEmpty()) {
                    val problemsTab = ProblemsTab()
                    problemsTab.addTo(myLayout)

                    ApplicationManager.getApplication().invokeLater {
                        myLayout.selectAndFocus(problemsTab.getContent(), true, true)
                    }

                    val panel = ProblemsPanel(myProject, problems)
                    problemsTab.panel.addToCenter(panel)
                }
            }
        }
    }

    override fun icon(): Icon = AllIcons.Vcs.Push
}
