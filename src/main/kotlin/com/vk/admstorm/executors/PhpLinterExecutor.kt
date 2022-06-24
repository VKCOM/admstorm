package com.vk.admstorm.executors

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.vk.admstorm.configuration.problems.panels.ProblemsPanel
import com.vk.admstorm.executors.tabs.ProblemsTab
import com.vk.admstorm.parsers.PhpLinterWarningsParser
import com.vk.admstorm.ui.MyIcons
import javax.swing.Icon

class PhpLinterExecutor(project: Project, command: String) :
    BaseRunnableExecutor(Config(tabName = "PHP Linter", command = command), project) {

    private val myProblemsTab = ProblemsTab()

    init {
        withTab(myProblemsTab)
    }

    override fun onFinish() {
        invokeLater {
            selectTab(myProblemsTab)
        }

        invokeLater {
            val problems = PhpLinterWarningsParser.parse(project, output().stderr)
            val panel = ProblemsPanel(project, problems)
            myProblemsTab.panel.addToCenter(panel)
        }
    }

    override fun icon(): Icon = MyIcons.phpLinter
}
