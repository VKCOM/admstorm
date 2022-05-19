package com.vk.admstorm.settings

import com.intellij.openapi.options.Configurable

/**
 * Provides controller functionality for application settings.
 */
class AdmStormSettingsConfigurable : Configurable {
    private var mySettingsComponent = AdmStormSettingsComponent()

    override fun getDisplayName() = "AdmStorm"
    override fun getPreferredFocusedComponent() = mySettingsComponent.preferredFocusedComponent
    override fun createComponent() = mySettingsComponent.mainPanel()

    override fun isModified(): Boolean {
        val settings = AdmStormSettingsState.getInstance()
        return mySettingsComponent.syncBranchCheckout != settings.needSyncBranchCheckout ||
                mySettingsComponent.gitConflictResolutionStrategy != settings.gitConflictResolutionStrategy ||
                mySettingsComponent.checkSyncOnFocus != settings.checkSyncOnFocus ||
                mySettingsComponent.connectWhenProjectStarts != settings.connectWhenProjectStarts ||
                mySettingsComponent.runPhpLinterAsInTeamcity != settings.runPhpLinterAsInTeamcityWhenPushToGitlab ||
                mySettingsComponent.autoPushToServerAfterCommit != settings.autoPushToServerAfterCommit ||
                mySettingsComponent.askYubikeyPassword != settings.askYubikeyPassword
    }

    override fun apply() {
        val settings = AdmStormSettingsState.getInstance()
        settings.apply {
            needSyncBranchCheckout = mySettingsComponent.syncBranchCheckout
            gitConflictResolutionStrategy = mySettingsComponent.gitConflictResolutionStrategy
            checkSyncOnFocus = mySettingsComponent.checkSyncOnFocus
            connectWhenProjectStarts = mySettingsComponent.connectWhenProjectStarts
            runPhpLinterAsInTeamcityWhenPushToGitlab = mySettingsComponent.runPhpLinterAsInTeamcity
            autoPushToServerAfterCommit = mySettingsComponent.autoPushToServerAfterCommit
            askYubikeyPassword = mySettingsComponent.askYubikeyPassword
        }
    }

    override fun reset() {
        val settings = AdmStormSettingsState.getInstance()
        mySettingsComponent.apply {
            syncBranchCheckout = settings.needSyncBranchCheckout
            gitConflictResolutionStrategy = settings.gitConflictResolutionStrategy
            checkSyncOnFocus = settings.checkSyncOnFocus
            connectWhenProjectStarts = settings.connectWhenProjectStarts
            runPhpLinterAsInTeamcity = settings.runPhpLinterAsInTeamcityWhenPushToGitlab
            autoPushToServerAfterCommit = settings.autoPushToServerAfterCommit
            askYubikeyPassword = settings.askYubikeyPassword
        }
    }
}
