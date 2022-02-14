package com.vk.admstorm.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Supports storing the application settings in a persistent way.
 * The [State] and [Storage] annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(
    name = "com.vk.admstorm.settings.AdmStormSettingsState",
    storages = [Storage("AdmStormPlugin.xml")]
)
class AdmStormSettingsState : PersistentStateComponent<AdmStormSettingsState?> {
    companion object {
        fun getInstance(): AdmStormSettingsState {
            return ApplicationManager.getApplication().getService(AdmStormSettingsState::class.java)
        }
    }

    var needSyncBranchCheckout = true
    var gitConflictResolutionStrategy = GitConflictResolutionStrategy.Ask
    var checkSyncOnFocus = true
    var connectWhenProjectStarts = true
    var runPhpLinterAsInTeamcityWhenPushToGitlab = false
    var autoPushToServerAfterCommit = true

    override fun getState() = this

    override fun loadState(state: AdmStormSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
