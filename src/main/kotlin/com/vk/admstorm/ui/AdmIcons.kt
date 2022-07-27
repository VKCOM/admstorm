package com.vk.admstorm.ui

import com.intellij.ui.IconManager

object AdmIcons {
    private fun icon(name: String) = IconManager.getInstance().getIcon(name, javaClass)

    object General {
        val Kphp = icon("/icons/kphp.svg")
        val KphpBench = icon("/icons/kphp-bench.svg")
        val KhpLinter = icon("/icons/php-linter.svg")
        val Yarn = icon("/icons/yarn")
        val ToolWorking = icon("/icons/tool-working")
        val ToolError = icon("/icons/tool-error")
        val ToolStopped = icon("/icons/tool-stopped")
        val Logs = icon("/icons/show-logs")
        val ExternalLinkArrow = icon("/icons/external_link_arrow.svg")
    }

    object Service {
        val Default = icon("/icons/service/default")
        val Sentry = icon("/icons/service/sentry")
        val Jira = icon("/icons/service/jira")
        val Confluence = icon("/icons/service/confluence")
        val GitLab = icon("/icons/service/gitlab")
        val TeamCity = icon("/icons/service/teamcity")
        val Grafana = icon("/icons/service/grafana")
        val Pmcmanager = icon("/icons/service/pmcmanager")
        val Statshouse = icon("/icons/service/statshouse")
    }
}
