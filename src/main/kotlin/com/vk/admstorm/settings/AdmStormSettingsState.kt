package com.vk.admstorm.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import com.vk.admstorm.git.GitConflictResolutionStrategy

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
        fun getInstance() = service<AdmStormSettingsState>()
    }

    var needSyncBranchCheckout = true
    var checkoutConflictResolutionStrategy = GitConflictResolutionStrategy.Ask
    var checkSyncOnFocus = true
    var connectWhenProjectStarts = true
    var runPhpLinterAsInTeamcityWhenPushToGitlab = false
    var pushToServerAfterCommit = true
    var askYubikeyPassword = true
    var showYarnWatchWidget = true
    var showWatchDebugLogWidget = true
    var userNameForSentry = ""
    var localDeployConfig = false

    override fun getState() = this

    override fun loadState(state: AdmStormSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
