package com.vk.admstorm.executors

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import javax.swing.Icon

class DeployTestDomainExecutor(project: Project, command: String) :
    BaseRunnableExecutor(Config(tabName = "Deploy test domain", layoutName = "Deploy test domain", command), project) {

    private var onReadyCallback = {}

    override fun onFinish() {
        onReadyCallback()
    }

    fun withOnReadyCallback(callback: () -> Unit): DeployTestDomainExecutor {
        onReadyCallback = callback
        return this
    }

    override fun icon(): Icon = AllIcons.Nodes.Deploy
    override fun runnerTitle() = "Deploy test domain"
    override fun runnerId() = "DeployTestDomain"
}
