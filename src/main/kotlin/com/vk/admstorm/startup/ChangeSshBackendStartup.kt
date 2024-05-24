package com.vk.admstorm.startup

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.openapi.project.Project
import com.intellij.ssh.config.SshConnectionConfigService
import com.vk.admstorm.notifications.AdmNotification

object ChangeSshBackendStartup {
    private val LOG = logger<ChangeSshBackendStartup>()

    private const val LEGACY_SSH_NOTIFICATOR_OPTION = "sshLegacyChanges"
    private const val SSH_NOTIFICATOR_OPTION = "sshOpenSSHChanges"
    private const val SSH_CONFIG_BACKEND = "ssh.config.backend"

    private fun changeSshConfiguration(configValue: SshConnectionConfigService.Kind) {
        AdvancedSettings.setEnum(SSH_CONFIG_BACKEND, configValue)
    }

    fun changeConfigurationProcess(project: Project) {
        val prpCmp = PropertiesComponent.getInstance(project)

        val dntShow = prpCmp.getBoolean(SSH_NOTIFICATOR_OPTION)
        if (dntShow) {
            LOG.info("OPENSSH option was enabled")
            return
        }

        //TODO: Remove it after 2 month
        if (prpCmp.getValue(LEGACY_SSH_NOTIFICATOR_OPTION) != null) {
            prpCmp.unsetValue(LEGACY_SSH_NOTIFICATOR_OPTION)
        }

        val sshSettingValue = AdvancedSettings.getEnum(SSH_CONFIG_BACKEND, SshConnectionConfigService.Kind::class.java)
        if (sshSettingValue == SshConnectionConfigService.Kind.OPENSSH) {
            prpCmp.setValue(SSH_NOTIFICATOR_OPTION, true)
            LOG.info("OPENSSH option was enabled before")
            return
        }

        changeSshConfiguration(SshConnectionConfigService.Kind.OPENSSH)
        LOG.info("LEGACY SSH has been changed to OPENSSH")

        AdmNotification("We changed your ssh type to OPENSSH")
            .withActions(
                AdmNotification.Action("Don`t show it again") { _, notification ->
                    LOG.info("OPENSSH option has been set")
                    notification.expire()
                }
            ).withActions(
                AdmNotification.Action("Rollback and turn off this notification") { _, notification ->
                    changeSshConfiguration(SshConnectionConfigService.Kind.LEGACY)
                    LOG.info("SSH option changed to LEGACY")
                    notification.expire()
                }
            ).show(project)

        prpCmp.setValue(LEGACY_SSH_NOTIFICATOR_OPTION, true)
    }
}
