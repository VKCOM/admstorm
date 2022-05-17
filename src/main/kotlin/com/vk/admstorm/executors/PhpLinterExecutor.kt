package com.vk.admstorm.executors

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.vk.admstorm.configuration.problems.panels.ProblemsPanel
import com.vk.admstorm.executors.tabs.ProblemsTab
import com.vk.admstorm.parsers.PhpLinterWarningsParser
import com.vk.admstorm.ui.MyIcons
import javax.swing.Icon

class PhpLinterExecutor(project: Project, command: String) :
    BaseRunnableExecutor(Config(name = "PHP Linter", command = command), project) {

    private val myProblemsTab = ProblemsTab()

    init {
        withTab(myProblemsTab)
    }

    override fun onReady() {
        invokeLater {
            myLayout.selectAndFocus(myProblemsTab.content, true, true)
        }

        invokeLater {
            val problems = PhpLinterWarningsParser.parse(myProject, myOutputListener.output.stderr)
            val panel = ProblemsPanel(myProject, problems)
            myProblemsTab.panel.addToCenter(panel)
        }
    }

    override fun icon(): Icon = MyIcons.phpLinter
}
