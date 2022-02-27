package com.vk.admstorm.executors

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.vk.admstorm.utils.ServerNameProvider
import javax.swing.Icon

class RunAnythingExecutor(project: Project, command: String) :
    BaseRunnableExecutor(Config(name = "Run '$command' on ${ServerNameProvider.name()}", command = command), project) {

    override fun onReady() {}

    override fun icon(): Icon = AllIcons.RunConfigurations.Compound
}
