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
                mySettingsComponent.autoPushToServerAfterCommit != settings.autoPushToServerAfterCommit
    }

    override fun apply() {
        val settings = AdmStormSettingsState.getInstance()
        settings.needSyncBranchCheckout = mySettingsComponent.syncBranchCheckout
        settings.gitConflictResolutionStrategy = mySettingsComponent.gitConflictResolutionStrategy
        settings.checkSyncOnFocus = mySettingsComponent.checkSyncOnFocus
        settings.connectWhenProjectStarts = mySettingsComponent.connectWhenProjectStarts
        settings.runPhpLinterAsInTeamcityWhenPushToGitlab = mySettingsComponent.runPhpLinterAsInTeamcity
        settings.autoPushToServerAfterCommit = mySettingsComponent.autoPushToServerAfterCommit
    }

    override fun reset() {
        val settings = AdmStormSettingsState.getInstance()
        mySettingsComponent.syncBranchCheckout = settings.needSyncBranchCheckout
        mySettingsComponent.gitConflictResolutionStrategy = settings.gitConflictResolutionStrategy
        mySettingsComponent.checkSyncOnFocus = settings.checkSyncOnFocus
        mySettingsComponent.connectWhenProjectStarts = settings.connectWhenProjectStarts
        mySettingsComponent.runPhpLinterAsInTeamcity = settings.runPhpLinterAsInTeamcityWhenPushToGitlab
        mySettingsComponent.autoPushToServerAfterCommit = settings.autoPushToServerAfterCommit
    }
}
