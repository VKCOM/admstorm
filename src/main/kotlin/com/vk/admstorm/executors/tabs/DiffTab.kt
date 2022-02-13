package com.vk.admstorm.executors.tabs

import com.intellij.openapi.project.Project
import com.vk.admstorm.executors.SimpleComponentWithActions
import com.vk.admstorm.playground.KphpPhpDiffViewer

class DiffTab(project: Project, name: String) : BaseTab(name) {
    val viewer = KphpPhpDiffViewer(project)

    override fun componentWithActions() =
        SimpleComponentWithActions(viewer.component, viewer.component)

    override fun componentToFocus() = viewer.component

    override fun afterAdd() = viewer.rediff()
}
