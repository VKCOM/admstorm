package com.vk.admstorm.actions

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.ui.GotItTooltip
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.or
import com.intellij.util.TextFieldCompletionProvider
import com.intellij.util.textCompletion.TextFieldWithCompletion
import com.jetbrains.php.lang.PhpLanguage
import com.vk.admstorm.env.Env
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.services.DeployTestDomainService
import com.vk.admstorm.ui.MessageDialog
import com.vk.admstorm.utils.MyUiUtils.bindText
import com.vk.admstorm.utils.MyUtils.runBackground
import java.awt.Dimension
import java.awt.Point
import java.util.*
import javax.swing.JComponent
import javax.swing.JLabel

class DeployTestDomainAction : AdmActionBase() {
    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        val options = DeployOptionsDialog.requestOptions(e.project!!) ?: return
        DeployTestDomainService.getInstance(e.project!!).deploy(options) { deployedDomain, deployed ->
            if (!deployed) {
                AdmWarningNotification("Test domain deployment failed for '$deployedDomain', see console output for details")
                    .withTitle("Deploy test domain")
                    .show()
                return@deploy
            }
            AdmNotification("Successfully deployed to '$deployedDomain' domain")
                .withTitle("Deploy test domain")
                .show()
        }
    }

    class DeployOptionsDialog(private val project: Project) : DialogWrapper(project) {
        companion object {
            fun requestOptions(project: Project): DeployTestDomainService.Options? {
                val dialog = DeployOptionsDialog(project)
                if (!dialog.showAndGet()) {
                    return null
                }
                return dialog.model
            }
        }

        private lateinit var centralPanel: DialogPanel
        val model = DeployTestDomainService.Options(
            isPublicDomain = false,
            domain = "",
            branch = "",
        )

        init {
            title = "Deploy Test Domain"

            setOKButtonText("Deploy")

            init()
        }

        override fun createCenterPanel(): JComponent {
            val tooltip = GotItTooltip(
                "org.vk.admstorm.deploy.test.domain.release.and.site.got.it.tooltip",
                "You can release domain or open site with free domains",
                project
            )

            lateinit var useCustomDomainCheckBox: Cell<JBRadioButton>
            lateinit var useLastUsedDomainCheckBox: Cell<JBRadioButton>
            lateinit var domainCellTextField: Cell<LanguageTextField>

            val repository = GitUtils.getCurrentRepository(project) ?: return JLabel("No repository found")
            val currentBranch = repository.currentBranchName ?: return JLabel("No current branch found")

            model.branch = currentBranch
            val branchTextField = createBranchCompletionTextField(project)
            val domainTextField = LanguageTextField(PhpLanguage.INSTANCE, project, "", true)
            centralPanel = panel {
                row {
                    checkBox("Public domain")
                        .bindSelected(model::isPublicDomain)
                }

                buttonsGroup {
                    row {
                        radioButton("Auto domain")
                            .actionListener { _, component ->
                                centralPanel.apply()
                                if (component.isSelected) {
                                    model.domain = ""
                                }
                                centralPanel.reset()

                                Timer().schedule(object : TimerTask() {
                                    override fun run() {
                                        this@DeployOptionsDialog.pack()
                                    }
                                }, 50)
                            }
                            .apply {
                                component.isSelected = true
                            }
                        useCustomDomainCheckBox = radioButton("Custom domain")
                            .actionListener { _, component ->
                                centralPanel.apply()
                                if (component.isSelected) {
                                    model.domain = ""
                                    domainTextField.grabFocus()

                                    tooltip.show(domainCellTextField.comment!!) { _, _ -> Point(140, 45) }
                                }
                                centralPanel.reset()
                            }
                        useLastUsedDomainCheckBox = radioButton("Last used domain")
                            .actionListener { _, component ->
                                centralPanel.apply()
                                if (component.isSelected) {
                                    model.domain =
                                        DeployTestDomainService.getInstance(project).getLastUsedDomain() ?: ""

                                    tooltip.show(domainCellTextField.comment!!) { _, _ -> Point(140, 45) }
                                }
                                centralPanel.reset()
                            }
                    }.bottomGap(BottomGap.SMALL)
                }

                row("Domain:") {
                    domainCellTextField = cell(domainTextField)
                        .bindText(model::domain)
                        .horizontalAlign(HorizontalAlign.FILL)
                        .comment(
                            "<a href='release'>Release</a> this domain. Open <a href='${Env.data.testDomainSite}'>domains</a>",
                            200
                        ) {
                            centralPanel.apply()

                            if (it.url != null) {
                                BrowserUtil.browse(it.url)
                                return@comment
                            }

                            runBackground(project, "Release ${model.domain} domain") {
                                if (DeployTestDomainService.getInstance(project).releaseDomain(model)) {
                                    MessageDialog.showSuccess(
                                        "Domain ${model.domain} successfully released",
                                        "Domain released"
                                    )
                                } else {
                                    MessageDialog.showWarning(
                                        "Problem with release ${model.domain} domain. Perhaps name is wrong?",
                                        "Problem with domain release"
                                    )
                                }
                            }
                        }
                }.visibleIf(useCustomDomainCheckBox.selected.or(useLastUsedDomainCheckBox.selected))
                    .bottomGap(BottomGap.SMALL)

                row("Branch:") {
                    cell(branchTextField)
                        .bindText(model::branch)
                        .horizontalAlign(HorizontalAlign.FILL)
                        .comment("Use <a href='current'>current</a> branch", 100) {
                            centralPanel.apply()
                            model.branch = currentBranch
                            centralPanel.reset()
                        }
                }.bottomGap(BottomGap.SMALL)
            }.apply {
                preferredSize = Dimension(400, -1)
            }

            val disposable = Disposer.newDisposable()
            centralPanel.registerValidators(disposable)
            Disposer.register(this.project, disposable)

            centralPanel.reset()
            return centralPanel
        }

        private fun createBranchCompletionTextField(project: Project): TextFieldWithCompletion {
            return TextFieldWithCompletion(
                project,
                BranchTextFieldCompletionProvider(project),
                "", true, true, true
            )
        }

        private class BranchTextFieldCompletionProvider(
            private val project: Project,
        ) : TextFieldCompletionProvider(), DumbAware {
            override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {
                val repository = GitUtils.getCurrentRepository(project) ?: return
                repository.branches.localBranches.forEach {
                    result.addElement(LookupElementBuilder.create(it.name))
                }
            }
        }
    }

    override fun beforeUpdate(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
    }
}
