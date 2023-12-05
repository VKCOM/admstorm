package com.vk.admstorm.git.sync.branches

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.*
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.utils.MyUtils
import com.vk.admstorm.utils.ServerNameProvider
import java.awt.event.ItemEvent
import javax.swing.JComponent
import javax.swing.JLabel

class SyncBranchesDialog(
    private val project: Project,
    private val localBranch: String,
    private val remoteBranch: String,
    private val onCancelSync: Runnable?,
    private val onSyncFinish: Runnable,
) : DialogWrapper(project, true) {
    private lateinit var branchComboBox: Cell<ComboBox<String>>
    private val helperLabel = JLabel("Branch will be changed on server")

    init {
        title = "Branches not Synchronized"

        setOKButtonText("Sync")
        setSize(380, 150)
        init()
    }

    private fun doLocalCheckoutToRemoteBranch() {
        LocalBranchSwitcher(project)
            .switch(remoteBranch, onSyncFinish)
    }

    private fun doRemoteCheckoutToLocalBranch() {
        RemoteBranchSwitcher(project, onCancelSync)
            .switch(localBranch, false, onSyncFinish)
    }

    private fun copyTextAndShowNotification(text: String) {
        invokeLater {
            MyUtils.copyToClipboard(text)
            AdmNotification("Branch name '$text' copied").show()
        }
    }

    override fun doOKAction() {
        super.doOKAction()

        val useRemoteBranch = branchComboBox.component.selectedItem == remoteBranch
        if (useRemoteBranch) {
            doLocalCheckoutToRemoteBranch()
        } else {
            doRemoteCheckoutToLocalBranch()
        }
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label("${ServerNameProvider.uppercase()} branch:")
            }
            indent {
                row {
                    link(remoteBranch) { copyTextAndShowNotification(remoteBranch) }
                }
            }

            row {
                label("Local branch:")
            }
            indent {
                row {
                    link(localBranch) { copyTextAndShowNotification(localBranch) }
                }
            }

            separator()

            row {
                label("Select main branch to sync:")
            }
            row {
                val branches = listOf(localBranch, remoteBranch)

                branchComboBox = comboBox(branches)
                    .align(AlignX.FILL)
                    .applyToComponent {
                        addItemListener {
                            if (it.stateChange == ItemEvent.SELECTED) {
                                helperLabel.text = if (it.item == remoteBranch) {
                                    "Branch will be changed locally"
                                } else {
                                    "Branch will be changed on ${ServerNameProvider.name()}"
                                }
                            }
                        }
                    }
            }
            row {
                cell(helperLabel)
            }
        }
    }
}
