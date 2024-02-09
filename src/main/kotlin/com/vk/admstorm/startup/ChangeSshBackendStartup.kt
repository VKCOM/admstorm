package com.vk.admstorm.startup

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.openapi.project.Project
import com.intellij.ssh.config.SshConnectionConfigService
import com.vk.admstorm.notifications.AdmNotification

object ChangeSshBackendStartup {
    private val LOG = logger<ChangeSshBackendStartup>()

    private const val SSH_NOTIFICATOR_OPTION = "sshLegacyChanges"
    private const val SSH_CONFIG_BACKEND = "ssh.config.backend"

    private fun changeSshConfiguration(configValue: SshConnectionConfigService.Kind) {
        AdvancedSettings.setEnum(SSH_CONFIG_BACKEND, configValue)
    }

    fun changeConfigurationProcess(project: Project) {
        val dntShow = PropertiesComponent.getInstance(project).getBoolean(SSH_NOTIFICATOR_OPTION)
        if (dntShow) {
            LOG.info("LEGACY SSH option was enabled")
            return
        }

        val sshSettingValue = AdvancedSettings.getEnum(SSH_CONFIG_BACKEND, SshConnectionConfigService.Kind::class.java)
        if (sshSettingValue == SshConnectionConfigService.Kind.LEGACY) {
            PropertiesComponent.getInstance(project).setValue(SSH_NOTIFICATOR_OPTION, true)
            LOG.info("LEGACY SSH option was enabled before")
            return
        }

        changeSshConfiguration(SshConnectionConfigService.Kind.LEGACY)
        LOG.info("LEGACY SSH has been changed")

        AdmNotification("We changed your ssh type to LEGACY")
            .withActions(
                AdmNotification.Action("Don`t show it again") { _, notification ->
                    LOG.info("LEGACY SSH option has been set")
                    notification.expire()
                }
            ).withActions(
                AdmNotification.Action("Rollback and turn off this notification") { _, notification ->
                    changeSshConfiguration(SshConnectionConfigService.Kind.OPENSSH)
                    LOG.info("SSH option changed to OpenSSH")
                    notification.expire()
                }
            ).show(project)

        PropertiesComponent.getInstance(project).setValue(SSH_NOTIFICATOR_OPTION, true)
    }
}
