package com.vk.admstorm.configuration.kbench

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.TextFieldCompletionProvider
import com.intellij.util.textCompletion.TextFieldWithCompletion
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.PhpIcons
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.completion.PhpCompletionUtil
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.vk.admstorm.utils.KBenchUtils
import java.awt.event.ActionListener
import javax.swing.*

open class KBenchConfigurationEditor(private val myProject: Project) : SettingsEditor<KBenchConfiguration>() {
    private var myPanel: JPanel? = null
    private var myBenchType: ComboBox<String>? = null
    private var myCountRuns: JTextField? = null

    private var myClassPanel: JPanel? = null
    private var myMethodPanel: JPanel? = null
    private var myBenchmemPanel: JPanel? = null

    private var myClassRadioButton: JBRadioButton? = null
    private var myMethodRadioButton: JBRadioButton? = null

    private var myClassTextField: LanguageTextField? = null
    private var myMethodTextField: TextFieldWithCompletion? = null
    private var myBenchmemCheckBox: JBCheckBox? = null

    override fun resetEditorFrom(demoRunConfiguration: KBenchConfiguration) {
        myClassRadioButton!!.isSelected = !demoRunConfiguration.isMethodScope
        myMethodRadioButton!!.isSelected = demoRunConfiguration.isMethodScope

        myClassTextField!!.text = demoRunConfiguration.className
        myMethodTextField!!.text = demoRunConfiguration.method

        myBenchType!!.selectedItem = demoRunConfiguration.benchType.command
        myBenchmemCheckBox!!.isSelected = demoRunConfiguration.benchmem

        myCountRuns!!.text = demoRunConfiguration.countRuns
    }

    override fun applyEditorTo(demoRunConfiguration: KBenchConfiguration) {
        demoRunConfiguration.isMethodScope = myMethodRadioButton!!.isSelected

        demoRunConfiguration.className = myClassTextField!!.text
        demoRunConfiguration.method = myMethodTextField!!.text

        demoRunConfiguration.benchType = KBenchType.from(myBenchType!!.selectedItem as String)
        demoRunConfiguration.benchmem = myBenchmemCheckBox!!.isSelected

        demoRunConfiguration.countRuns = myCountRuns!!.text
    }

    override fun createEditor(): JComponent = myPanel!!

    fun createUIComponents() {
        myMethodTextField = TextFieldWithCompletion(
            myProject,
            createMethodCompletionProvider(myProject), "", true, true, true
        )

        myClassTextField = LanguageTextField(PhpLanguage.INSTANCE, myProject, "")

        PhpCompletionUtil.installClassCompletion(myClassTextField!!, null, {}, { phpClass: PhpClass ->
            KBenchUtils.isBenchmarkClass(phpClass)
        })
    }

    private fun createMethodCompletionProvider(project: Project) =
        MyTextFieldCompletionProvider(this, project)

    private class MyTextFieldCompletionProvider(
        private val myParent: KBenchConfigurationEditor,
        private val myProject: Project
    ) : TextFieldCompletionProvider(), DumbAware {

        override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {
            val className = myParent.myClassTextField!!.text
            val selectedClass = PhpIndex.getInstance(myProject).getAnyByFQN(className).firstOrNull()

            if (selectedClass != null) {
                PhpClassHierarchyUtils.processMethods(
                    selectedClass,
                    selectedClass,
                    { method: Method, _: PhpClass, _: PhpClass ->
                        if (KBenchUtils.isBenchmarkMethod(method)) {
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

    private fun updatePanelsVisibility() {
        if (myClassRadioButton!!.isSelected) {
            setVisibleComponents(method = false)
        } else if (myMethodRadioButton!!.isSelected) {
            setVisibleComponents(method = true)
        }

        val isKphpBench = myBenchType?.selectedItem == KBenchType.Bench.command
        myBenchmemPanel?.isVisible = isKphpBench
    }

    private fun setVisibleComponents(method: Boolean) {
        myMethodPanel?.isVisible = method
    }

    init {
        val group = ButtonGroup()
        group.add(myClassRadioButton)
        group.add(myMethodRadioButton)

        val updateStateActionListener = ActionListener { updatePanelsVisibility() }

        myClassRadioButton?.addActionListener(updateStateActionListener)
        myMethodRadioButton?.addActionListener(updateStateActionListener)

        myClassTextField?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                updatePanelsVisibility()
            }
        })

        val boxModel = DefaultComboBoxModel<String>()
        listOf("bench", "bench-php", "bench-vs-php").forEach {
            boxModel.addElement(it)
        }

        myBenchType?.model = boxModel
        myBenchType?.selectedItem = "bench"
        myBenchType?.addActionListener(updateStateActionListener)

        myPanel?.border = IdeBorderFactory.createTitledBorder("Configuration")

        setVisibleComponents(method = false)
    }
}
