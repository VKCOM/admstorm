package com.vk.admstorm.services

import com.intellij.execution.Output
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.env.Env
import com.vk.admstorm.executors.DeployTestDomainExecutor

@Service
class DeployTestDomainService(private val project: Project) {
    companion object {
        private val LOG = logger<DeployTestDomainService>()
        private const val PROPERTY_ID = "admstorm.deploy.test.domain"
        fun getInstance(project: Project) = project.service<DeployTestDomainService>()
    }

    data class Options(
        var isPublicDomain: Boolean,
        var domain: String,
        var branch: String,
    )

    fun deploy(options: Options, onReady: (String?, Boolean) -> Unit) {
        val domain = options.domain.ifEmpty { "auto" }

        val deployCommand = if (options.isPublicDomain) {
            Env.data.deployPublicTestDomainCommand
        } else {
            Env.data.deployTestDomainCommand
        }
        val command = "$deployCommand $domain ${options.branch}"
        val executor = DeployTestDomainExecutor(project, command)

        executor.onReady {
            val output = executor.output()

            val deployedDomain = if (domain == "auto") {
                getDeployedDomain(output)
            } else {
                domain
            }
            if (deployedDomain == null) {
                LOG.warn("Can't parse domain name from command output")
                val wasDeployed = output.exitCode == 0
                onReady(null, wasDeployed)
                return@onReady
            }

            if (output.exitCode != 0) {
                LOG.warn("Problem with deploy test domain: ${output.stderr}")
                onReady(deployedDomain, false)
                return@onReady
            }

            setLastUsedDomain(deployedDomain)
            onReady(deployedDomain, true)
        }

        executor.run()
    }

    fun releaseDomain(options: Options): Boolean {
        val releaseCommand = if (options.isPublicDomain) {
            Env.data.clearPublicTestDomainCommand
        } else {
            Env.data.clearTestDomainCommand
        }
        val command = "$releaseCommand ${options.domain}"
        return CommandRunner.runRemotely(project, command).exitCode == 0
    }

    fun getLastUsedDomain(): String? {
        return PropertiesComponent.getInstance(project).getValue(PROPERTY_ID)
    }

    private fun getDeployedDomain(output: Output): String? {
        output.stdout.split("\n").forEach {
            if (it.contains("manage_tags: Tag ")) {
                return it.split("manage_tags: Tag ")
                    .getOrNull(1)
                    ?.split(" ")
                    ?.getOrNull(0)
                    ?.removePrefix("td.") ?: return@forEach
            }
        }

        return null
    }

    private fun setLastUsedDomain(newLastUsedDomain: String) {
        if (getLastUsedDomain() == newLastUsedDomain) {
            return
        }

        PropertiesComponent.getInstance(project).setValue(PROPERTY_ID, newLastUsedDomain)
    }
}
