package com.vk.admstorm.startup

import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.InstalledPluginsState
import com.intellij.ide.plugins.PluginManagerConfigurable
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.vk.admstorm.AdmService
import com.vk.admstorm.notifications.AdmNotification
import com.intellij.openapi.application.invokeLater
import com.vk.admstorm.utils.ServerNameProvider

object PluginsUpdateStartup {
    private val KPHPSTORM_PLUGIN_ID = PluginId.getId("com.vk.kphpstorm")
    private val MODULITE_PLUGIN_ID = PluginId.getId("com.vk.modulite")
    private val ADMSTORM_PLUGIN_ID = AdmService.PLUGIN_ID

    private val PLUGINS = mapOf(
        ADMSTORM_PLUGIN_ID to "https://vkcom.github.io/admstorm/whatsnew.html?server_name=${ServerNameProvider.name()}",
        KPHPSTORM_PLUGIN_ID to null,
        MODULITE_PLUGIN_ID to null,
    )

    private fun hasNewVersion(pluginID: PluginId): Boolean {
        return InstalledPluginsState.getInstance().hasNewerVersion(pluginID)
    }

    fun checkUpdates(project: Project) {
        for ((pluginID, changelogURL) in PLUGINS) {
            val pluginDescriptor = PluginManagerCore.getPlugin(pluginID)
            if (pluginDescriptor == null || !pluginDescriptor.isEnabled) {
                continue
            }

            if (!hasNewVersion(pluginID)) {
                continue
            }

            val pluginName = pluginDescriptor.name
            val updateNotification = AdmNotification("New version of the plugin is available")
                .withTitle(pluginName)
                .withActions(
                    AdmNotification.Action("Update") { _, _ ->
                        invokeLater {
                            ShowSettingsUtil.getInstance().showSettingsDialog(
                                project,
                                PluginManagerConfigurable::class.java,
                            ) {
                                it.openInstalledTab(pluginName)
                            }
                        }
                    }
                )

            if (changelogURL == null) {
                updateNotification.show()
                continue
            }

            updateNotification.withActions(
                AdmNotification.Action("What's new") { _, _ ->
                    BrowserUtil.browse(changelogURL)
                }
            ).show(project)
        }
    }
}
