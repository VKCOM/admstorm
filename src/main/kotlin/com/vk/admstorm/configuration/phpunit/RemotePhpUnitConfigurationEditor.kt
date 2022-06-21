package com.vk.admstorm.configuration.phpunit

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.LanguageTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.not
import com.intellij.ui.layout.or
import com.intellij.util.TextFieldCompletionProvider
import com.intellij.util.indexing.DumbModeAccessType
import com.intellij.util.textCompletion.TextFieldWithCompletion
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.PhpIcons
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.completion.PhpCompletionUtil
import com.jetbrains.php.lang.PhpLangUtil
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.phpunit.PhpUnitUtil
import com.jetbrains.php.run.PhpRunUtil
import com.vk.admstorm.utils.MyUiUtils.bindText
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JRadioButton

open class RemotePhpUnitConfigurationEditor(val project: Project) :
    SettingsEditor<RemotePhpUnitConfiguration>() {

    data class Model(
        var scope: PhpUnitScope = PhpUnitScope.Directory,

        var directory: String = "",
        var className: String = "",
        var methodName: String = "",
        var filename: String? = "",

        var phpUnitExe: String = "",
        var phpUnitConfig: String = "",
        var additionalParameters: String = "",
    )

    private var myFileTextCombo = ComboBox<String>()
    private lateinit var mainPanel: DialogPanel
    private val model = Model()

    override fun createEditor(): JComponent = component()

    fun component(): JPanel {
        lateinit var directoryRadioButton: Cell<JRadioButton>
        lateinit var classRadioButton: Cell<JRadioButton>
        lateinit var methodRadioButton: Cell<JRadioButton>

        val classTextField = LanguageTextField(PhpLanguage.INSTANCE, project, "")
        PhpCompletionUtil.installClassCompletion(classTextField, null, {}, {
            PhpUnitUtil.isTestClass(it)
        })

        val methodTextField = TextFieldWithCompletion(
            project,
            MethodCompletionProvider(),
            "", true, true, true
        )

        mainPanel = panel {
            group("Test Runner") {
                buttonsGroup {
                    row("Scope:") {
                        directoryRadioButton = radioButton("Directory", PhpUnitScope.Directory)
                            .horizontalAlign(HorizontalAlign.LEFT)
                            .apply {
                                component.isSelected = true
                            }

                        classRadioButton = radioButton("Class", PhpUnitScope.Class)
                            .horizontalAlign(HorizontalAlign.LEFT)
                        methodRadioButton = radioButton("Method", PhpUnitScope.Method)
                            .horizontalAlign(HorizontalAlign.LEFT)
                    }.bottomGap(BottomGap.SMALL)
                }.bind(model::scope)

                row("Directory:") {
                    textFieldWithBrowseButton(
                        "Select PHPUnit Tests Folder",
                        project,
                        FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    )
                        .horizontalAlign(HorizontalAlign.FILL)
                        .bindText(model::directory)
                }.visibleIf(directoryRadioButton.selected)
                    .bottomGap(BottomGap.SMALL)

                row("Class:") {
                    cell(classTextField)
                        .horizontalAlign(HorizontalAlign.FILL)
                        .bindText(model::className)
                }.visibleIf(classRadioButton.selected.or(methodRadioButton.selected))
                    .bottomGap(BottomGap.SMALL)

                row("Method:") {
                    cell(methodTextField)
                        .horizontalAlign(HorizontalAlign.FILL)
                        .bindText(model::methodName)
                }.visibleIf(methodRadioButton.selected)
                    .bottomGap(BottomGap.SMALL)

                row("File:") {
                    cell(myFileTextCombo)
                        .horizontalAlign(HorizontalAlign.FILL)
                        .bindItemNullable(model::filename)
                }.visibleIf(directoryRadioButton.selected.not())
                    .bottomGap(BottomGap.SMALL)
            }.bottomGap(BottomGap.NONE)

            group("Command Line") {
                row("PHPUnit Executable:") {
                    textFieldWithBrowseButton(
                        "Select PHPUnit Executable",
                        project,
                        FileChooserDescriptorFactory.createSingleFileDescriptor()
                    )
                        .horizontalAlign(HorizontalAlign.FILL)
                        .bindText(model::phpUnitExe)
                }
                row("Configuration file:") {
                    textFieldWithBrowseButton(
                        "Select PHPUnit Configuration File",
                        project,
                        FileChooserDescriptorFactory.createSingleFileDescriptor(XmlFileType.INSTANCE)
                    )
                        .horizontalAlign(HorizontalAlign.FILL)
                        .bindText(model::phpUnitConfig)
                }
            }.topGap(TopGap.NONE)
        }

        classTextField.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                mainPanel.reset()

                fillFileFieldVariants(project, PhpLangUtil.toFQN(model.className))
            }
        })

        return mainPanel
    }

    override fun resetEditorFrom(demoRunConfiguration: RemotePhpUnitConfiguration) {
        with(model) {
            scope = demoRunConfiguration.scope
            directory = demoRunConfiguration.directory
            className = demoRunConfiguration.className
            methodName = demoRunConfiguration.methodName
            filename = demoRunConfiguration.filename
            myFileTextCombo.addItem(demoRunConfiguration.filename)

            phpUnitExe = demoRunConfiguration.phpUnitExe
            phpUnitConfig = demoRunConfiguration.phpUnitConfig
            additionalParameters = demoRunConfiguration.additionalParameters
        }

        mainPanel.reset()
    }

    override fun applyEditorTo(demoRunConfiguration: RemotePhpUnitConfiguration) {
        mainPanel.apply()

        with(demoRunConfiguration) {
            scope = model.scope
            directory = model.directory
            className = model.className
            methodName = model.methodName

            filename = model.filename ?: ""
            phpUnitExe = model.phpUnitExe
            phpUnitConfig = model.phpUnitConfig
            additionalParameters = model.additionalParameters
        }
    }

    private fun fillFileFieldVariants(project: Project, fqn: String) {
        val oldText = model.filename
        val pathsWithClassByFqn: Set<String> =
            DumbModeAccessType.RELIABLE_DATA_ONLY.ignoreDumbMode<Set<String>, RuntimeException> {
                PhpIndex.getInstance(project).getClassesByFQN(fqn)
                    .filter { it != null && PhpUnitUtil.isTestClass(it) }
                    .map { it.containingFile.virtualFile.path }
                    .toSet()
            }

        if (pathsWithClassByFqn.isEmpty()) {
            return
        }

        myFileTextCombo.removeAllItems()

        pathsWithClassByFqn.forEach { path ->
            myFileTextCombo.addItem(path)

            if (oldText == path) {
                myFileTextCombo.selectedItem = path
            }
        }
    }

    private inner class MethodCompletionProvider :
        TextFieldCompletionProvider(), DumbAware {

        override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {
            val selectedFile = PhpRunUtil.findPsiFile(project, model.filename, true) ?: return

            val selectedClass =
                DumbModeAccessType.RELIABLE_DATA_ONLY.ignoreDumbMode<PhpClass?, RuntimeException> {
                    PhpUnitUtil.findClassByFQNInFile(model.className, selectedFile, project)
                } as PhpClass

            PhpClassHierarchyUtils.processMethods(
                selectedClass,
                selectedClass,
                { method: Method, _: PhpClass, _: PhpClass ->
                    if (PhpUnitUtil.isTestMethod(selectedClass, method)) {
                        result.addElement(LookupElementBuilder.create(method).withIcon(PhpIcons.PHP_TEST_METHOD))
                    }
                    true
                },
                false,
                false
            )
        }
    }
}
