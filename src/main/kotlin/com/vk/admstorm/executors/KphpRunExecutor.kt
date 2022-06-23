package com.vk.admstorm.executors

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.vk.admstorm.configuration.kphp.KphpRunType
import com.vk.admstorm.configuration.problems.panels.ProblemsPanel
import com.vk.admstorm.executors.tabs.ProblemsTab
import com.vk.admstorm.parsers.KphpErrorsParser
import com.vk.admstorm.ui.MyIcons
import javax.swing.Icon

class KphpRunExecutor(project: Project, type: KphpRunType, command: String) :
    BaseRunnableExecutor(Config(tabName = "KPHP ${type.command}", command = command), project) {

    private val myCompilationErrorsTab = ProblemsTab("Compilation errors")

    init {
        withTab(myCompilationErrorsTab)
    }

    override fun onReady() {
        invokeLater {
            myLayout.selectAndFocus(myCompilationErrorsTab.content, true, true)
        }

        invokeLater {
            val problems = KphpErrorsParser.parse(myProject, myOutputListener.output.stdout)
            val panel = ProblemsPanel(myProject, problems)
            myCompilationErrorsTab.panel.addToCenter(panel)
        }
    }

    override fun icon(): Icon = MyIcons.kphp
}
