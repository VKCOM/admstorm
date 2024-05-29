package com.vk.admstorm

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileTypes.ex.FileTypeChooser
import com.intellij.openapi.fileTypes.impl.AbstractFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.registry.Registry
import com.vk.admstorm.diagnostic.AdmStormLoggerFactory
import com.vk.admstorm.highlight.CppTypeHighlightPatcher
import com.vk.admstorm.notifications.AdmErrorNotification
import com.vk.admstorm.services.SentryService
import com.vk.admstorm.settings.AdmStormSettingsState
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.startup.ChangeSshBackendStartup
import com.vk.admstorm.startup.PluginsUpdateStartup
import com.vk.admstorm.utils.MyUtils.measureTime
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

        //TODO: remove it after release AdmStorm on Windows
        if (SystemInfo.isWindows) {
            AdmErrorNotification("AdmStorm is not available on Windows yet").show()
        }

        ChangeSshBackendStartup.changeConfigurationProcess(project)

        measureTime(LOG, "patch cpp highlight") {
            val cppType = FileTypeChooser.getKnownFileTypeOrAssociate(".c") as AbstractFileType
            CppTypeHighlightPatcher.patch(cppType)
        }

        if (AdmStormSettingsState.getInstance().connectWhenProjectStarts) {
            SshConnectionService.getInstance(project).tryConnectSilence {
                AdmStartupService.getInstance(project).afterConnectionTasks()
            }
        }

        PluginsUpdateStartup.checkUpdates(project)

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
}
