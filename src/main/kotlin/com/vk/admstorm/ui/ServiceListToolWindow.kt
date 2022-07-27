package com.vk.admstorm.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.Gaps
import com.vk.admstorm.env.Env
import com.vk.admstorm.env.EnvListener
import com.vk.admstorm.utils.extensions.pluginEnabled
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JPanel

class ServiceListToolWindow : ToolWindowFactory {
    companion object {
        private val serviceToIcon = mapOf(
            "jira" to AdmIcons.Service.Jira,
            "confluence" to AdmIcons.Service.Confluence,
            "gitlab" to AdmIcons.Service.GitLab,
            "sentry" to AdmIcons.Service.Sentry,
            "teamcity" to AdmIcons.Service.TeamCity,
            "grafana" to AdmIcons.Service.Grafana,
            "pmc_manager" to AdmIcons.Service.Pmcmanager,
            "statshouse" to AdmIcons.Service.Statshouse,
        )

        private val defaultIcon = AdmIcons.Service.Default
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentManager = toolWindow.contentManager

        if (project.pluginEnabled()) {
            val content = contentManager.factory.createContent(toolWindowPanel(project), null, false)
            contentManager.addContent(content)
        }
    }

    private fun showServicesPanel(): DialogPanel {
        return panel {
            customize(Gaps(0, 15, 0, 0))

            val services = Env.data.services
            services.forEach { service ->
                row {
                    val icon = serviceToIcon.getOrDefault(service.key, defaultIcon)
                    icon(icon)

                    browserLink(service.name, service.url)
                }
            }
        }
    }

    private fun toolWindowPanel(project: Project): JComponent {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))

        if (Env.isResolved() && panel.components.isEmpty()) {
            panel.add(showServicesPanel())
        }

        project.messageBus.connect().subscribe(EnvListener.TOPIC, object : EnvListener {
            override fun onResolve() {
                if (panel.components.isEmpty()) {
                    panel.add(showServicesPanel())
                }
            }
        })

        return panel
    }
}
