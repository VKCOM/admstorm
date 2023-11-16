package com.vk.admstorm.ui

import com.intellij.ui.IconManager

object AdmIcons {
    private fun icon(name: String) = IconManager.getInstance().getIcon(name, javaClass.classLoader)

    object General {
        val Kphp = icon("/icons/kphp.svg")
        val KphpBench = icon("/icons/kphp-bench.svg")
        val KhpLinter = icon("/icons/php-linter.svg")
        val Yarn = icon("/icons/yarn.svg")
        val ToolWorking = icon("/icons/tool-working.svg")
        val ToolError = icon("/icons/tool-error.svg")
        val ToolStopped = icon("/icons/tool-stopped.svg")
        val Logs = icon("/icons/show-logs.svg")
        val ExternalLinkArrow = icon("/icons/external_link_arrow.svg")
    }

    object Service {
        val Confluence = icon("/icons/service/confluence.svg")
        val Default = icon("/icons/service/default.svg")
        val Enginestat = icon("/icons/service/enginestat.svg")
        val GitLab = icon("/icons/service/gitlab.svg")
        val Grafana = icon("/icons/service/grafana.svg")
        val Ingria = icon("/icons/service/ingria.svg")
        val Jira = icon("/icons/service/jira.svg")
        val Pmcmanager = icon("/icons/service/pmcmanager.svg")
        val Sentry = icon("/icons/service/sentry.svg")
        val Statshouse = icon("/icons/service/statshouse.svg")
        val TeamCity = icon("/icons/service/teamcity.svg")
        val Watchdogs = icon("/icons/service/watchdogs.svg")
        val Hastebin = icon("/icons/service/hastebin.svg")
    }
}
