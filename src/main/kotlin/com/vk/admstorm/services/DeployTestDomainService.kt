package com.vk.admstorm.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.env.Env
import com.vk.admstorm.executors.DeployTestDomainExecutor

@Service
class DeployTestDomainService(private val project: Project) {
    companion object {
        fun getInstance(project: Project) = project.service<DeployTestDomainService>()
    }

    data class Options(
        var useCustomDomain: Boolean,
        var domain: String,
        var branch: String,
    )

    fun deploy(options: Options) {
        val domain = if (options.useCustomDomain) {
            options.domain
        } else {
            "auto"
        }

        val command = "${Env.data.deployTestDomainCommand} $domain ${options.branch}"
        DeployTestDomainExecutor(project, command).run()
    }

    fun releaseDomain(name: String): Boolean {
        val command = "${Env.data.clearTestDomainCommand}} $name"
        return CommandRunner.runRemotely(project, command).exitCode == 0
    }
}
