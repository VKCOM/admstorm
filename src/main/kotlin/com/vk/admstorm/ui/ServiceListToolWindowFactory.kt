package com.vk.admstorm.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.Gaps
import com.intellij.util.application
import com.vk.admstorm.env.Env
import com.vk.admstorm.env.EnvListener
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JPanel

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
        val content = contentManager.factory.createContent(toolWindowPanel(), null, false)

        val ex = toolWindow as ToolWindowEx
        ex.stretchWidth(0)

        contentManager.addContent(content)
    }

    private fun showServicesPanel(): DialogPanel {
        return panel {
            customize(Gaps(0, 15, 0, 0))

            val services = Env.data.services
            services.forEach { service ->
                row {
                    val icon = serviceToIcon.getOrDefault(service.name, defaultIcon)
                    icon(icon)

                    browserLink(service.name, service.url)
                }
            }
        }

    }

    private fun toolWindowPanel(): JComponent {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))

        if (Env.isResolved() && panel.components.isEmpty()) {
            panel.add(showServicesPanel())
        }

        application.messageBus.connect().subscribe(EnvListener.TOPIC, object : EnvListener {
            override fun resolveChanged() {
                if (panel.components.isEmpty()) {
                    panel.add(showServicesPanel())
                }
            }
        })

        return panel
    }
}
