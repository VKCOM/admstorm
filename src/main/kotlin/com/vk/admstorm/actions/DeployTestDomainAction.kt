package com.vk.admstorm.actions

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.TextFieldCompletionProvider
import com.intellij.util.textCompletion.TextFieldWithCompletion
import com.jetbrains.php.lang.PhpLanguage
import com.vk.admstorm.services.DeployTestDomainService
import com.vk.admstorm.ui.MessageDialog
import com.vk.admstorm.utils.MyUiUtils.bindText
import com.vk.admstorm.utils.MyUtils.runBackground
import git4idea.branch.GitBranchUtil
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel

class DeployTestDomainAction : AdmActionBase() {
    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        val options = DeployOptionsDialog.requestOptions(e.project!!) ?: return
        DeployTestDomainService.getInstance(e.project!!).deploy(options)
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
            useCustomDomain = false,
            isPublicDomain = false,
            domain = "",
            branch = "",
        )

        init {
            title = "Deploy Test Domain"
            init()
        }

        override fun createCenterPanel(): JComponent {
            lateinit var useCustomDomainCheckBox: Cell<JBCheckBox>

            val repository = GitBranchUtil.getCurrentRepository(project) ?: return JLabel("No repository found")
            val currentBranch = repository.currentBranchName ?: return JLabel("No current branch found")

            model.branch = currentBranch
            val branchTextField = createBranchCompletionTextField(project)
            val domainTextField = LanguageTextField(PhpLanguage.INSTANCE, project, "", true)
            centralPanel = panel {
                row {
                    checkBox("Public domain")
                        .bindSelected(model::isPublicDomain)
                }

                row {
                    useCustomDomainCheckBox = checkBox("Use custom domain")
                        .bindSelected(model::useCustomDomain)
                }.bottomGap(BottomGap.SMALL)

                row("Domain:") {
                    cell(domainTextField)
                        .bindText(model::domain)
                        .horizontalAlign(HorizontalAlign.FILL)
                        .comment("<a href='release'>Release</a> current domain?", 100) {
                            centralPanel.apply()
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
                }.visibleIf(useCustomDomainCheckBox.selected)
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
                val repository = GitBranchUtil.getCurrentRepository(project) ?: return
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
