package com.vk.admstorm.settings

import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBRadioButton
import com.vk.admstorm.env.Env
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JPanel

class AdmStormSettingsComponent {
    private lateinit var myMainPanel: JPanel
    private lateinit var myBranchSyncSettingsPanel: JPanel
    private lateinit var myPushToGitlabPanel: JPanel

    private lateinit var mySyncBranchCheckoutCheckBox: JBCheckBox

    private lateinit var myConflictAskRadioButton: JBRadioButton
    private lateinit var myConflictStashRadioButton: JBRadioButton
    private lateinit var myConflictForceRadioButton: JBRadioButton

    private lateinit var myCheckSyncOnFocusCheckBox: JBCheckBox
    private lateinit var myConnectWhenProjectStartsCheckBox: JBCheckBox

    private lateinit var myRunPhpLinterAsInTeamcityCheckBox: JBCheckBox

    val preferredFocusedComponent: JComponent
        get() = mySyncBranchCheckoutCheckBox

    var syncBranchCheckout: Boolean
        get() = mySyncBranchCheckoutCheckBox.isSelected
        set(value) {
            mySyncBranchCheckoutCheckBox.isSelected = value
        }

    var gitConflictResolutionStrategy: GitConflictResolutionStrategy
        get() = when {
            myConflictAskRadioButton.isSelected -> GitConflictResolutionStrategy.Ask
            myConflictStashRadioButton.isSelected -> GitConflictResolutionStrategy.Stash
            myConflictForceRadioButton.isSelected -> GitConflictResolutionStrategy.ForceCheckout
            else -> GitConflictResolutionStrategy.Ask
        }
        set(value) {
            when (value) {
                GitConflictResolutionStrategy.Ask -> myConflictAskRadioButton.isSelected = true
                GitConflictResolutionStrategy.Stash -> myConflictStashRadioButton.isSelected = true
                GitConflictResolutionStrategy.ForceCheckout -> myConflictForceRadioButton.isSelected = true
            }
        }

    var checkSyncOnFocus: Boolean
        get() = myCheckSyncOnFocusCheckBox.isSelected
        set(value) {
            myCheckSyncOnFocusCheckBox.isSelected = value
        }

    var connectWhenProjectStarts: Boolean
        get() = myConnectWhenProjectStartsCheckBox.isSelected
        set(value) {
            myConnectWhenProjectStartsCheckBox.isSelected = value
        }

    var runPhpLinterAsInTeamcity: Boolean
        get() = myRunPhpLinterAsInTeamcityCheckBox.isSelected
        set(value) {
            myRunPhpLinterAsInTeamcityCheckBox.isSelected = value
        }

    init {
        val group = ButtonGroup()
        group.add(myConflictAskRadioButton)
        group.add(myConflictForceRadioButton)
        group.add(myConflictStashRadioButton)

        myConflictAskRadioButton.isSelected = true

        myBranchSyncSettingsPanel.border = IdeBorderFactory.createTitledBorder("Branch synchronization")
        mySyncBranchCheckoutCheckBox.text =
            "Automatically switch branches on ${Env.data.serverName.ifEmpty { "server" }}"

        myPushToGitlabPanel.border = IdeBorderFactory.createTitledBorder("Push to Gitlab")
    }

    fun mainPanel() = myMainPanel
}
