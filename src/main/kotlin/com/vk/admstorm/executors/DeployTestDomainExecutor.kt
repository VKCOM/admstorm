package com.vk.admstorm.executors

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project

class DeployTestDomainExecutor(project: Project, private val command: String) :
    BaseRemoteExecutor(project, "Deploy test domain") {

    private var onReady = {}

    override fun onFinish() {
        onReady()
    }

    fun onReady(callback: () -> Unit): DeployTestDomainExecutor {
        onReady = callback
        return this
    }

    override fun layoutName() = "Deploy test domain"

    override fun tabName() = "Deploy test domain"

    override fun command() = command

    override fun icon() = AllIcons.Nodes.Deploy

    override fun runnerTitle() = "Deploy test domain"

    override fun runnerId() = "DeployTestDomain"
}
