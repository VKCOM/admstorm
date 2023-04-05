package com.vk.admstorm.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.vk.admstorm.git.GitConflictResolutionStrategy
import com.vk.admstorm.ui.StatusBarUtils
import com.vk.admstorm.ui.WatchDebugLogStatusBarWidgetFactory
import com.vk.admstorm.ui.YarnWatchStatusBarWidgetFactory
import com.vk.admstorm.utils.ServerNameProvider

/**
 * Provides controller functionality for application settings.
 */
class AdmStormSettingsConfigurable(private val project: Project) : Configurable {
    data class Model(
        var connectWhenProjectStarts: Boolean,
        var checkSyncOnFocus: Boolean,
        var syncBranchCheckout: Boolean,
        var checkoutConflictResolutionStrategy: GitConflictResolutionStrategy,
        var pushToServerAfterCommit: Boolean,
        var runPhpLinterAsInTeamcity: Boolean,
        var askYubikeyPassword: Boolean,
        var showYarnWatchWidget: Boolean,
        var showWatchDebugLogWidget: Boolean,
        var userNameForSentry: String,
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
        showYarnWatchWidget = true,
        showWatchDebugLogWidget = true,
        userNameForSentry = "",
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

            group("Status Bar Tools") {
                row {
                    checkBox("Enable yarn watch widget")
                        .comment("Shows the yarn watch status bar widget.")
                        .bindSelected(model::showYarnWatchWidget)
                }
                row {
                    checkBox("Enable watch debug log widget")
                        .comment("Shows the watch debug log status bar widget.")
                        .bindSelected(model::showWatchDebugLogWidget)
                }
            }

            group("Additional") {
                row {
                    checkBox("Ask password for Yubikey when it is automatically reset")
                        .bindSelected(model::askYubikeyPassword)
                }
            }

            group("Sentry Error Reporting") {
                row("User name:") {
                    textField()
                        .comment("Will be used to identify the user in Sentry. It is recommended to use the same name as in ${ServerNameProvider.name()}.")
                        .bindText(model::userNameForSentry)
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
                model.askYubikeyPassword != settings.askYubikeyPassword ||
                model.showYarnWatchWidget != settings.showYarnWatchWidget ||
                model.showWatchDebugLogWidget != settings.showWatchDebugLogWidget ||
                model.userNameForSentry != settings.userNameForSentry
    }

    override fun apply() {
        mainPanel.apply()

        val settings = AdmStormSettingsState.getInstance()
        with(settings) {
            needSyncBranchCheckout = model.syncBranchCheckout
            checkoutConflictResolutionStrategy = model.checkoutConflictResolutionStrategy
            checkSyncOnFocus = model.checkSyncOnFocus
            connectWhenProjectStarts = model.connectWhenProjectStarts
            runPhpLinterAsInTeamcityWhenPushToGitlab = model.runPhpLinterAsInTeamcity
            pushToServerAfterCommit = model.pushToServerAfterCommit
            askYubikeyPassword = model.askYubikeyPassword
            showYarnWatchWidget = model.showYarnWatchWidget
            showWatchDebugLogWidget = model.showWatchDebugLogWidget
            userNameForSentry = model.userNameForSentry
        }

        StatusBarUtils.setEnabled(project, YarnWatchStatusBarWidgetFactory.FACTORY_ID, model.showYarnWatchWidget)
        StatusBarUtils.setEnabled(project, WatchDebugLogStatusBarWidgetFactory.FACTORY_ID, model.showWatchDebugLogWidget)
    }

    override fun reset() {
        val settings = AdmStormSettingsState.getInstance()

        // Users can change settings in a context menu of the status bar, so need check.
        val actualYarnWatchVisibility = StatusBarUtils.getEnabled(project, YarnWatchStatusBarWidgetFactory.FACTORY_ID)
        val actualWatchDebugLogVisibility = StatusBarUtils.getEnabled(project, WatchDebugLogStatusBarWidgetFactory.FACTORY_ID)

        if (settings.showYarnWatchWidget != actualYarnWatchVisibility) {
            settings.showYarnWatchWidget = actualYarnWatchVisibility
        }
        if (settings.showWatchDebugLogWidget != actualWatchDebugLogVisibility) {
            settings.showWatchDebugLogWidget = actualWatchDebugLogVisibility
        }

        with(model) {
            syncBranchCheckout = settings.needSyncBranchCheckout
            checkoutConflictResolutionStrategy = settings.checkoutConflictResolutionStrategy
            checkSyncOnFocus = settings.checkSyncOnFocus
            connectWhenProjectStarts = settings.connectWhenProjectStarts
            runPhpLinterAsInTeamcity = settings.runPhpLinterAsInTeamcityWhenPushToGitlab
            pushToServerAfterCommit = settings.pushToServerAfterCommit
            askYubikeyPassword = settings.askYubikeyPassword
            showYarnWatchWidget = settings.showYarnWatchWidget
            showWatchDebugLogWidget = settings.showWatchDebugLogWidget
            userNameForSentry = settings.userNameForSentry
        }

        mainPanel.reset()
    }
}
