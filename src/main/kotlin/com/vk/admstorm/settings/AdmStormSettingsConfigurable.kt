package com.vk.admstorm.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.vk.admstorm.git.GitConflictResolutionStrategy
import com.vk.admstorm.utils.ServerNameProvider

/**
 * Provides controller functionality for application settings.
 */
class AdmStormSettingsConfigurable : Configurable {
    data class Model(
        var connectWhenProjectStarts: Boolean,
        var checkSyncOnFocus: Boolean,
        var syncBranchCheckout: Boolean,
        var checkoutConflictResolutionStrategy: GitConflictResolutionStrategy,
        var pushToServerAfterCommit: Boolean,
        var runPhpLinterAsInTeamcity: Boolean,
        var askYubikeyPassword: Boolean,
    )

    private val mainPanel: DialogPanel
    private val model = Model(
        connectWhenProjectStarts = false,
        checkSyncOnFocus = false,
        syncBranchCheckout = false,
        checkoutConflictResolutionStrategy = GitConflictResolutionStrategy.Ask,
        pushToServerAfterCommit = false,
        runPhpLinterAsInTeamcity = false,
        askYubikeyPassword = false,
    )

    init {
        mainPanel = panel {
            row {
                checkBox("Automatically connect to ${ServerNameProvider.name()} when the project starts")
                    .bindSelected(model::connectWhenProjectStarts)
            }

            row {
                checkBox("Check for sync when the focus was switched to IDE")
                    .comment("Each time you collapse and expand the IDE plugin will check the synchronization.", 80)
                    .bindSelected(model::checkSyncOnFocus)
            }

            group("Automatic Synchronization") {
                row {
                    checkBox("Automatically switch branches on ${ServerNameProvider.name()}")
                        .comment("Switch occurs after a successful local switch to a other branch.")
                        .bindSelected(model::syncBranchCheckout)
                }
                indent {
                    buttonsGroup {
                        row("Default conflict resolution strategy:") {
                            radioButton("Ask", GitConflictResolutionStrategy.Ask)
                            radioButton("Stash", GitConflictResolutionStrategy.Stash)
                            radioButton("Force checkout", GitConflictResolutionStrategy.ForceCheckout)
                        }
                            .bottomGap(BottomGap.SMALL)
                            .rowComment("When switching to a new branch, conflicts may arise, some files may be lost. The setting above allows you to set the default strategy for such cases.")
                    }.bind(model::checkoutConflictResolutionStrategy)
                }
                row {
                    checkBox("Automatically push new commit to the ${ServerNameProvider.name()} after a successful commit")
                        .comment("After a successful commit, it will immediately be sent to the ${ServerNameProvider.name()}.")
                        .bindSelected(model::pushToServerAfterCommit)
                }
            }

            group("Push to Gitlab") {
                row {
                    checkBox("Run PHP Linter as in Teamcity")
                        .comment("Slows down the linter by about 2 times, but the results will definitely match the results on Teamcity.")
                        .bindSelected(model::runPhpLinterAsInTeamcity)
                }
            }

            group("Additional") {
                row {
                    checkBox("Ask password for Yubikey when it is automatically reset")
                        .bindSelected(model::askYubikeyPassword)
                }
            }
        }
    }

    override fun getDisplayName() = "AdmStorm"
    override fun getPreferredFocusedComponent() = mainPanel
    override fun createComponent() = mainPanel

    override fun isModified(): Boolean {
        mainPanel.apply()

        val settings = AdmStormSettingsState.getInstance()
        return model.syncBranchCheckout != settings.needSyncBranchCheckout ||
                model.checkoutConflictResolutionStrategy != settings.checkoutConflictResolutionStrategy ||
                model.checkSyncOnFocus != settings.checkSyncOnFocus ||
                model.connectWhenProjectStarts != settings.connectWhenProjectStarts ||
                model.runPhpLinterAsInTeamcity != settings.runPhpLinterAsInTeamcityWhenPushToGitlab ||
                model.pushToServerAfterCommit != settings.pushToServerAfterCommit ||
                model.askYubikeyPassword != settings.askYubikeyPassword
    }

    override fun apply() {
        mainPanel.apply()

        val settings = AdmStormSettingsState.getInstance()
        settings.apply {
            needSyncBranchCheckout = model.syncBranchCheckout
            checkoutConflictResolutionStrategy = model.checkoutConflictResolutionStrategy
            checkSyncOnFocus = model.checkSyncOnFocus
            connectWhenProjectStarts = model.connectWhenProjectStarts
            runPhpLinterAsInTeamcityWhenPushToGitlab = model.runPhpLinterAsInTeamcity
            pushToServerAfterCommit = model.pushToServerAfterCommit
            askYubikeyPassword = model.askYubikeyPassword
        }
    }

    override fun reset() {
        val settings = AdmStormSettingsState.getInstance()
        model.apply {
            syncBranchCheckout = settings.needSyncBranchCheckout
            checkoutConflictResolutionStrategy = settings.checkoutConflictResolutionStrategy
            checkSyncOnFocus = settings.checkSyncOnFocus
            connectWhenProjectStarts = settings.connectWhenProjectStarts
            runPhpLinterAsInTeamcity = settings.runPhpLinterAsInTeamcityWhenPushToGitlab
            pushToServerAfterCommit = settings.pushToServerAfterCommit
            askYubikeyPassword = settings.askYubikeyPassword
        }

        mainPanel.reset()
    }
}
