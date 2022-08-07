package com.vk.admstorm.executors

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.vk.admstorm.configuration.problems.panels.ProblemsPanel
import com.vk.admstorm.executors.tabs.ProblemsTab
import com.vk.admstorm.parsers.PhpLinterWarningsParser
import com.vk.admstorm.ui.AdmIcons

class PhpLinterExecutor(project: Project, private val command: String) : BaseRemoteExecutor(project, "PHP Linter") {
    private val myProblemsTab = ProblemsTab()

    init {
        withTab(myProblemsTab)
    }

    override fun layoutName() = "PHP Linter"

    override fun tabName() = "PHP Linter"

    override fun command() = command

    override fun icon() = AdmIcons.General.KhpLinter

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
}
