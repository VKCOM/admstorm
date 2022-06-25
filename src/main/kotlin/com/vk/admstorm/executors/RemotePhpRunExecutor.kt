package com.vk.admstorm.executors

import com.intellij.openapi.project.Project
import icons.PhpIcons

class RemotePhpRunExecutor(project: Project, private val command: String) :
    BaseRemoteExecutor(project, "Remote PHP") {

    override fun layoutName() = "Remote PHP"

    override fun tabName() = "Remote PHP"

    override fun command() = command

    override fun icon() = PhpIcons.PhpRemote

    override fun onFinish() {}
}
