package com.vk.admstorm.git.sync.branches

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.ActionLink
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.utils.MyUtils
import com.vk.admstorm.utils.ServerNameProvider
import javax.swing.JLabel
import javax.swing.JPanel

class SyncBranchesDialog(
    private val myProject: Project,
    localBranch: String,
    remoteBranch: String,
    private var onCancelSync: Runnable?,
    private val onSyncFinish: Runnable
) : DialogWrapper(myProject, true) {

    private lateinit var contentPane: JPanel

    private lateinit var myRemoteBranchLabel: JLabel
    private lateinit var myRemoteBranchLabelLink: ActionLink
    private lateinit var myLocalBranchLabelLink: ActionLink

    private lateinit var mySelectMainBranchCombo: ComboBox<String>
    private lateinit var myHelpLabelForCheckBox: JLabel

    override fun doOKAction() {
        super.doOKAction()

        val useRemoteBranch = mySelectMainBranchCombo.selectedItem as String == myRemoteBranchLabelLink.text
        if (useRemoteBranch) {
            doLocalCheckoutToRemoteBranch()
        } else {
            doRemoteCheckoutToLocalBranch()
        }
    }

    private fun doLocalCheckoutToRemoteBranch() {
        LocalBranchSwitcher(myProject)
            .switch(myRemoteBranchLabelLink.text, onSyncFinish)
    }

    private fun doRemoteCheckoutToLocalBranch() {
        RemoteBranchSwitcher(myProject, onCancelSync)
            .switch(myLocalBranchLabelLink.text, false, onSyncFinish)
    }

    init {
        title = "Branches not Synchronized"

        myRemoteBranchLabelLink.text = remoteBranch
        myRemoteBranchLabelLink.addActionListener {
            copyTextAndShowNotification(remoteBranch)
        }

        myLocalBranchLabelLink.text = localBranch
        myLocalBranchLabelLink.addActionListener {
            copyTextAndShowNotification(localBranch)
        }

        mySelectMainBranchCombo.addItem(localBranch)
        mySelectMainBranchCombo.addItem(remoteBranch)

        myRemoteBranchLabel.text = "Adm branch:"
        myHelpLabelForCheckBox.text = "Branch will be changed in ${ServerNameProvider.name()}"

        mySelectMainBranchCombo.addItemListener { e ->
            val selected = e.itemSelectable.selectedObjects[0] as String
            if (selected == remoteBranch) {
                myHelpLabelForCheckBox.text = "Branch will be changed locally"
            } else {
                myHelpLabelForCheckBox.text = "Branch will be changed on ${ServerNameProvider.name()}"
            }
        }

        setOKButtonText("Sync")

        setSize(380, 150)

        init()
    }

    private fun copyTextAndShowNotification(text: String) {
        invokeLater {
            MyUtils.copyToClipboard(text)
            AdmNotification("Branch name $text copied").show()
        }
    }

    override fun createCenterPanel() = contentPane
}
