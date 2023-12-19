package com.vk.admstorm

import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.InstalledPluginsState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.utils.ServerNameProvider

data class PluginsHealthCard(val project: Project) {
    private val admId = AdmService.ADM_PLUGIN_ID
    private val kphpId = AdmService.KPHPSTORM_PLUGIN_ID
    private val moduliteId = AdmService.MODULITE_PLUGIN_ID

    private fun hasNewVersion(pluginId : PluginId): Boolean {
        return InstalledPluginsState.getInstance().hasNewerVersion(pluginId)
    }

    fun checkUpdates() {
        val hasAdmNewerVersion = hasNewVersion(admId)
        val hasKphpStormNewVersion = hasNewVersion(kphpId)
        val hasModuliteNewVersion = hasNewVersion(moduliteId)

        if (!(hasAdmNewerVersion || hasModuliteNewVersion || hasKphpStormNewVersion)) {
            return
        }

        var updateNotification = AdmNotification("New version of the plugin is available")
            .withActions(
                AdmNotification.Action("Update") { _, _ ->
                    invokeLater {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, "Plugins")
                    }
                }
            )

        if (hasAdmNewerVersion) {
            updateNotification = updateNotification.withActions(
                AdmNotification.Action("What's new") { _, _ ->
                    val url = "https://vkcom.github.io/admstorm/whatsnew.html?server_name=${ServerNameProvider.name()}"
                    BrowserUtil.browse(url)
                }
            )
        }
        updateNotification.show(project)
    }
}