package com.vk.admstorm.configuration.phpunit

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.TextFieldCompletionProvider
import com.intellij.util.indexing.DumbModeAccessType
import com.intellij.util.textCompletion.TextFieldWithCompletion
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.PhpIcons
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.completion.PhpCompletionUtil
import com.jetbrains.php.lang.PhpLangUtil
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.phpunit.PhpUnitUtil
import com.jetbrains.php.run.PhpRunUtil
import java.awt.event.ActionListener
import java.util.stream.Collectors
import javax.swing.ButtonGroup
import javax.swing.JPanel
import javax.swing.JTextField

open class RemotePhpUnitConfigurationEditor(private val myProject: Project) :
    SettingsEditor<RemotePhpUnitConfiguration>() {

    private var myPanel: JPanel? = null
    private var myDirectoryPanel: JPanel? = null
    private var myClassPanel: JPanel? = null
    private var myMethodPanel: JPanel? = null
    private var myFilePanel: JPanel? = null
    private var myAdditionalPanel: JPanel? = null

    private var myDirectoryRadioButton: JBRadioButton? = null
    private var myClassRadioButton: JBRadioButton? = null
    private var myMethodRadioButton: JBRadioButton? = null

    private var myDirectoryTextField: TextFieldWithBrowseButton? = null
    private var myClassTextField: LanguageTextField? = null
    private var myMethodTextField: TextFieldWithCompletion? = null
    private var myFileTextCombo: ComboBox<String>? = null

    private var myUseParatestCheckBox: JBCheckBox? = null
    private var myConfigurationFileTextField: TextFieldWithBrowseButton? = null
    private var myPhpUnitExeTextField: TextFieldWithBrowseButton? = null
    private var myAdditionalParameters: JTextField? = null

    override fun resetEditorFrom(demoRunConfiguration: RemotePhpUnitConfiguration) {
        myDirectoryRadioButton!!.isSelected = demoRunConfiguration.isDirectoryScope
        myClassRadioButton!!.isSelected = demoRunConfiguration.isClassScope
        myMethodRadioButton!!.isSelected = demoRunConfiguration.isMethodScope

        myDirectoryTextField!!.text = demoRunConfiguration.directory
        myClassTextField!!.text = demoRunConfiguration.className
        myMethodTextField!!.text = demoRunConfiguration.method
        myFileTextCombo!!.addItem(demoRunConfiguration.filename)

        myUseParatestCheckBox!!.isSelected = demoRunConfiguration.useParatest
        myConfigurationFileTextField!!.text = demoRunConfiguration.configPath
        myPhpUnitExeTextField!!.text = demoRunConfiguration.phpUnitPath
        myAdditionalParameters!!.text = demoRunConfiguration.additionalParameters
    }

    override fun applyEditorTo(demoRunConfiguration: RemotePhpUnitConfiguration) {
        demoRunConfiguration.isDirectoryScope = myDirectoryRadioButton!!.isSelected
        demoRunConfiguration.isClassScope = myClassRadioButton!!.isSelected
        demoRunConfiguration.isMethodScope = myMethodRadioButton!!.isSelected

        demoRunConfiguration.directory = myDirectoryTextField!!.text
        demoRunConfiguration.className = myClassTextField!!.text
        demoRunConfiguration.method = myMethodTextField!!.text
        demoRunConfiguration.filename = myFileTextCombo!!.selectedItem as String

        demoRunConfiguration.useParatest = myUseParatestCheckBox!!.isSelected
        demoRunConfiguration.configPath = myConfigurationFileTextField!!.text
        demoRunConfiguration.phpUnitPath = myPhpUnitExeTextField!!.text
        demoRunConfiguration.additionalParameters = myAdditionalParameters!!.text
    }

    override fun createEditor() = myPanel!!

    fun createUIComponents() {
        myMethodTextField =
            TextFieldWithCompletion(myProject, createMethodCompletionProvider(myProject), "", true, true, true)

        myClassTextField = LanguageTextField(PhpLanguage.INSTANCE, myProject, "")

        PhpCompletionUtil.installClassCompletion(myClassTextField!!, null, {}, { phpClass: PhpClass ->
            PhpUnitUtil.isTestClass(phpClass)
        })
    }

    private fun fillFileFieldVariants(project: Project, fqn: String) {
        val oldText = getSelectedFilePath()
        val pathsWithClassByFqn: Set<String> =
            DumbModeAccessType.RELIABLE_DATA_ONLY.ignoreDumbMode<Set<String>, RuntimeException> {
                PhpIndex.getInstance(project).getClassesByFQN(fqn).stream()
                    .filter { klass ->
                        klass == null || PhpUnitUtil.isTestClass(klass)
                    }
                    .map { klass -> klass.containingFile.virtualFile.path }
                    .collect(Collectors.toSet()) as Set<String>
            }

        if (pathsWithClassByFqn.isNotEmpty()) {
            myFileTextCombo!!.removeAllItems()
            pathsWithClassByFqn.forEach { path ->
                myFileTextCombo!!.addItem(path)
                if (oldText == path) {
                    myFileTextCombo!!.selectedItem = path
                }
            }
        }
    }

    private fun getSelectedFilePath() = myFileTextCombo?.selectedItem as String? ?: ""

    private fun createMethodCompletionProvider(project: Project) =
        MyTextFieldCompletionProvider(this, project)

    init {
        val group = ButtonGroup()
        group.add(myDirectoryRadioButton)
        group.add(myClassRadioButton)
        group.add(myMethodRadioButton)

        myDirectoryRadioButton?.isSelected = true

        myDirectoryTextField?.addBrowseFolderListener(
            "Select PHPUnit Folder", "Select PHPUnit test folder to run", null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )

        val updateStateActionListener = ActionListener { updateState() }

        myDirectoryRadioButton?.addActionListener(updateStateActionListener)
        myClassRadioButton?.addActionListener(updateStateActionListener)
        myMethodRadioButton?.addActionListener(updateStateActionListener)
        myFileTextCombo?.addActionListener(updateStateActionListener)
        myDirectoryTextField?.addActionListener(updateStateActionListener)

        myClassTextField?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                fillFileFieldVariants(myProject, PhpLangUtil.toFQN(myClassTextField!!.text))
                updateFieldsAvailability()
            }
        })

        myPanel?.border = IdeBorderFactory.createTitledBorder("Test Runner")
        myAdditionalPanel?.border = IdeBorderFactory.createTitledBorder("Other", false)

        myUseParatestCheckBox?.isVisible = false

        myConfigurationFileTextField?.addBrowseFolderListener(
            "Select PHPUnit Configuration", "Select PHPUnit Configuration to run", null,
            FileChooserDescriptorFactory.createSingleFileDescriptor("xml")
        )

        setVisibleComponents(directory = true, file = false, clazz = false, method = false)
    }

    private fun updatePanelsVisibility() {
        if (myDirectoryRadioButton!!.isSelected) {
            setVisibleComponents(directory = true, file = false, clazz = false, method = false)
        } else if (myClassRadioButton!!.isSelected) {
            setVisibleComponents(directory = false, file = true, clazz = true, method = false)
        } else if (myMethodRadioButton!!.isSelected) {
            setVisibleComponents(directory = false, file = true, clazz = true, method = true)
        }
    }

    private fun updateFieldsAvailability() {
        myDirectoryTextField!!.isEnabled = true
        myMethodTextField!!.isEnabled = true
        myClassTextField!!.isEnabled = true
        if (myMethodRadioButton!!.isSelected && (getSelectedFilePath().isEmpty() || myClassTextField!!.text.isEmpty())) {
            myMethodTextField!!.isEnabled = false
        }
        myFileTextCombo!!.isEnabled = myFileTextCombo!!.itemCount > 1
    }

    private fun updateState() {
        updatePanelsVisibility()
        updateFieldsAvailability()
    }

    private fun setVisibleComponents(directory: Boolean, file: Boolean, clazz: Boolean, method: Boolean) {
        myDirectoryPanel?.isVisible = directory
        myFilePanel?.isVisible = file
        myClassPanel?.isVisible = clazz
        myMethodPanel?.isVisible = method
    }

    private class MyTextFieldCompletionProvider(
        private val myParent: RemotePhpUnitConfigurationEditor,
        private val myProject: Project
    ) : TextFieldCompletionProvider(), DumbAware {

        override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {
            val selectedFile =
                PhpRunUtil.findPsiFile(myProject, myParent.getSelectedFilePath(), true) as? PhpFile ?: return

            val selectedClass =
                DumbModeAccessType.RELIABLE_DATA_ONLY.ignoreDumbMode<PhpClass?, RuntimeException> {
                    PhpUnitUtil.findClassByFQNInFile(myParent.myClassTextField!!.text, selectedFile, myProject)
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
