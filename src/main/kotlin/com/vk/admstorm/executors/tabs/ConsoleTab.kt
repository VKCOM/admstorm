package com.vk.admstorm.executors.tabs

import com.intellij.openapi.project.Project
import com.vk.admstorm.console.Console
import com.vk.admstorm.executors.SimpleComponentWithActions
import javax.swing.JComponent

class ConsoleTab(project: Project, name: String) : BaseTab(name) {
    val console = Console(project)

    override fun componentWithActions() =
        SimpleComponentWithActions(console.view() as JComponent?, console.component())

    override fun componentToFocus() = console.component()
}
