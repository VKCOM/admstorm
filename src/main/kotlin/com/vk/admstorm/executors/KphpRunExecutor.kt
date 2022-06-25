package com.vk.admstorm.executors

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.vk.admstorm.configuration.kphp.KphpRunType
import com.vk.admstorm.configuration.problems.panels.ProblemsPanel
import com.vk.admstorm.executors.tabs.ProblemsTab
import com.vk.admstorm.parsers.KphpErrorsParser
import com.vk.admstorm.ui.MyIcons

class KphpRunExecutor(
    project: Project,
    private val type: KphpRunType,
    private val command: String
) : BaseRemoteExecutor(project, "KPHP ${type.command}") {

    private val myCompilationErrorsTab = ProblemsTab("Compilation errors")

    init {
        withTab(myCompilationErrorsTab)
    }

    override fun layoutName() = "KPHP ${type.command}"

    override fun tabName() = "KPHP ${type.command}"

    override fun command() = command

    override fun icon() = MyIcons.kphp

    override fun onFinish() {
        invokeLater {
            selectTab(myCompilationErrorsTab)
        }

        invokeLater {
            val problems = KphpErrorsParser.parse(project, output().stdout)
            val panel = ProblemsPanel(project, problems)
            myCompilationErrorsTab.panel.addToCenter(panel)
        }
    }
}
