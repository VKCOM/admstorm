package com.vk.admstorm

import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.InstalledPluginsState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileTypes.ex.FileTypeChooser
import com.intellij.openapi.fileTypes.impl.AbstractFileType
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.registry.Registry
import com.vk.admstorm.diagnostic.AdmStormLoggerFactory
import com.vk.admstorm.highlight.CppTypeHighlightPatcher
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.services.SentryService
import com.vk.admstorm.settings.AdmStormSettingsState
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.startup.ChangeSshBackendStartup
import com.vk.admstorm.utils.MyUtils.measureTime
import com.vk.admstorm.utils.ServerNameProvider
import com.vk.admstorm.utils.extensions.pluginEnabled

class AdmStormStartupActivity : ProjectActivity {
    companion object {
        private val LOG = logger<AdmStormStartupActivity>()
    }

    override suspend fun execute(project: Project) {
        setupLogger(project)

        // TODO: move to using DumbService?
        invokeLater {
            initPlugin(project)
        }
    }

    private fun initPlugin(project: Project) {
        if (!project.pluginEnabled()) {
            // We don't connect if this is not a vkcom project
            return
        }

        ChangeSshBackendStartup.changeConfigurationProcess(project)
        checkUpdates(project)

        measureTime(LOG, "patch cpp highlight") {
            val cppType = FileTypeChooser.getKnownFileTypeOrAssociate(".c") as AbstractFileType
            CppTypeHighlightPatcher.patch(cppType)
        }

        if (AdmStormSettingsState.getInstance().connectWhenProjectStarts) {
            SshConnectionService.getInstance(project).tryConnectSilence {
                AdmStartupService.getInstance(project).afterConnectionTasks()
            }
        }

        // Это необходимо чтобы для бенчмарков показывались все пункты в списке
        // который открывается при клике на иконку рядом с классом или методом.
        val key = Registry.get("suggest.all.run.configurations.from.context")
        key.setValue(true)
    }

    private fun setupLogger(project: Project) {
        val defaultLoggerFactory = Logger.getFactory()
        val sentry = SentryService.getInstance(project)

        Logger.setFactory(AdmStormLoggerFactory(sentry, defaultLoggerFactory))
    }

    private fun hasNewVersion(pluginId : PluginId): Boolean {
        return InstalledPluginsState.getInstance().hasNewerVersion(pluginId)
    }

    private fun checkUpdates(project: Project) {
        val hasAdmNewerVersion = hasNewVersion(AdmService.ADM_PLUGIN_ID)
        val hasKphpStormNewVersion = hasNewVersion(AdmService.KPHPSTORM_PLUGIN_ID)
        val hasModuliteNewVersion = hasNewVersion(AdmService.MODULITE_PLUGIN_ID)

        if (!(hasAdmNewerVersion && hasModuliteNewVersion && hasKphpStormNewVersion)) {
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
