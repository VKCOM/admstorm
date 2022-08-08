package com.vk.admstorm.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.Gaps
import com.vk.admstorm.env.Env

class ServiceListToolWindowFactory : ToolWindowFactory {
    companion object {
        private val serviceToIcon = mapOf(
            "Jira" to AdmIcons.Service.Jira,
            "Confluence" to AdmIcons.Service.Confluence,
            "GitLab" to AdmIcons.Service.GitLab,
            "Sentry" to AdmIcons.Service.Sentry,
            "TeamCity" to AdmIcons.Service.TeamCity,
            "Grafana" to AdmIcons.Service.Grafana,
            "PMC Manager" to AdmIcons.Service.Pmcmanager,
        )

        private val defaultIcon = AdmIcons.Service.Default
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(createDialogPanel(), null, false)

        val ex = toolWindow as ToolWindowEx
        ex.stretchWidth(0)

        contentManager.addContent(content)
    }

    private fun createDialogPanel(): DialogPanel {
        val services = Env.data.services

        return panel {
            panel {
                customize(Gaps(0, 15, 0, 0))

                services.forEach { service ->
                    row {
                        val icon = serviceToIcon.getOrDefault(service.name, defaultIcon)
                        icon(icon)

                        browserLink(service.name, service.url)
                    }
                }
            }
        }
    }
}
