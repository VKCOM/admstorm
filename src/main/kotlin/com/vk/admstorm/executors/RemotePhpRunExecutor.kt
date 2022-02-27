package com.vk.admstorm.executors

import com.intellij.openapi.project.Project
import icons.PhpIcons
import javax.swing.Icon

class RemotePhpRunExecutor(project: Project, command: String) :
    BaseRunnableExecutor(Config(name = "Remote PHP", command = command), project) {

    override fun onReady() {}

    override fun icon(): Icon = PhpIcons.PhpRemote
}
