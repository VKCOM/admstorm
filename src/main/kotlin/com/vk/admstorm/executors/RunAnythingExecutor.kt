package com.vk.admstorm.executors

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.vk.admstorm.utils.ServerNameProvider

class RunAnythingExecutor(project: Project, private val command: String) :
    BaseRemoteExecutor(project, "Run '$command' on ${ServerNameProvider.name()}") {

    override fun layoutName() = "Run '$command' on ${ServerNameProvider.name()}"

    override fun tabName() = "Run '$command' on ${ServerNameProvider.name()}"

    override fun command() = command

    override fun icon() = AllIcons.RunConfigurations.Compound

    override fun onFinish() {}
}
