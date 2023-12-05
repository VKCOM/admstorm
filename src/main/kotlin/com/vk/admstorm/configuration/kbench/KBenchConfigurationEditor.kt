package com.vk.admstorm.configuration.kbench

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.LanguageTextField
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.and
import com.jetbrains.php.completion.PhpCompletionUtil
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.vk.admstorm.utils.MyUiUtils.bindText
import com.vk.admstorm.utils.MyUiUtils.selectedValueMatches
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JRadioButton

open class KBenchConfigurationEditor(private val myProject: Project) : SettingsEditor<KBenchConfiguration>() {
    data class Model(
        var scope: KBenchScope = KBenchScope.Class,
        var benchType: KBenchType? = KBenchType.Bench,

        var className: String = "",
        var methodName: String = "",

        var compareMethodName: String = "",
        var compareClassName: String = "",

        var benchmarkMemory: Boolean = false,
        var countIteration: Int = 5,
    )

    private lateinit var mainPanel: DialogPanel
    private val model = Model()

    override fun resetEditorFrom(demoRunConfiguration: KBenchConfiguration) {
        model.scope = demoRunConfiguration.scope
        model.benchType = demoRunConfiguration.benchType
        model.className = demoRunConfiguration.className
        model.methodName = demoRunConfiguration.methodName
        model.compareClassName = demoRunConfiguration.compareClassName
        model.compareMethodName = demoRunConfiguration.compareMethodName
        model.benchmarkMemory = demoRunConfiguration.benchmarkMemory
        model.countIteration = demoRunConfiguration.countIteration

        mainPanel.reset()
    }

    override fun applyEditorTo(demoRunConfiguration: KBenchConfiguration) {
        mainPanel.apply()

        demoRunConfiguration.scope = model.scope
        demoRunConfiguration.benchType = model.benchType!!
        demoRunConfiguration.className = model.className
        demoRunConfiguration.methodName = model.methodName
        demoRunConfiguration.compareClassName = model.compareClassName
        demoRunConfiguration.compareMethodName = model.compareMethodName
        demoRunConfiguration.benchmarkMemory = model.benchmarkMemory
        demoRunConfiguration.countIteration = model.countIteration
    }

    override fun createEditor(): JComponent = component()

    fun component(): JPanel {
        lateinit var classRadioButton: Cell<JRadioButton>
        lateinit var methodRadioButton: Cell<JRadioButton>
        lateinit var typeComboBox: Cell<ComboBox<KBenchType>>

        val classTextField = LanguageTextField(PhpLanguage.INSTANCE, myProject, "")
        PhpCompletionUtil.installClassCompletion(classTextField, null, {}, { klass: PhpClass ->
            KBenchUtils.isBenchmarkClass(klass) && klass.fqn.trim('\\') != model.compareClassName
        })

        val compareClassTextField = LanguageTextField(PhpLanguage.INSTANCE, myProject, "")
        PhpCompletionUtil.installClassCompletion(compareClassTextField, null, {}, { klass: PhpClass ->
            KBenchUtils.isBenchmarkClass(klass) && klass.fqn.trim('\\') != model.className
        })

        val methodTextField = KBenchUtils.createMethodCompletionTextField(myProject, { model.className }) {
            it.name != model.compareMethodName
        }
        val compareMethodTextField = KBenchUtils.createMethodCompletionTextField(myProject, { model.className }) {
            it.name != model.methodName
        }

        mainPanel = panel {
            buttonsGroup {
                row("Scope:") {
                    classRadioButton = radioButton("Class", KBenchScope.Class)
                        .align(AlignX.LEFT)
                        .apply {
                            component.isSelected = true
                        }
                    methodRadioButton = radioButton("Method", KBenchScope.Method)
                        .align(AlignX.LEFT)
                }.bottomGap(BottomGap.SMALL)
            }.bind(model::scope)

            row("Type:") {
                typeComboBox = comboBox(
                    KBenchType.values().toList(),
                    SimpleListCellRenderer.create(KBenchType.Bench.presentation) { it.presentation }
                )
                    .align(AlignX.FILL)
                    .bindItem(model::benchType)
            }.bottomGap(BottomGap.SMALL)

            row("Class:") {
                cell(classTextField)
                    .align(AlignX.FILL)
                    .bindText(model::className)
            }.bottomGap(BottomGap.SMALL)

            row("Method:") {
                cell(methodTextField)
                    .align(AlignX.FILL)
                    .bindText(model::methodName)
            }.visibleIf(methodRadioButton.selected)
                .bottomGap(BottomGap.SMALL)

            row("Compare class:") {
                cell(compareClassTextField)
                    .align(AlignX.FILL)
                    .bindText(model::compareClassName)
            }.visibleIf(typeComboBox.selectedValueMatches { it == KBenchType.BenchAb }.and(classRadioButton.selected))
                .bottomGap(BottomGap.SMALL)

            row("Compare method:") {
                cell(compareMethodTextField)
                    .align(AlignX.FILL)
                    .bindText(model::compareMethodName)
            }.visibleIf(methodRadioButton.selected.and(typeComboBox.selectedValueMatches { it == KBenchType.BenchAb }))
                .bottomGap(BottomGap.SMALL)

            row {
                checkBox("Benchmark memory allocations")
                    .bindSelected(model::benchmarkMemory)
            }.visibleIf(typeComboBox.selectedValueMatches { it == KBenchType.Bench })
                .bottomGap(BottomGap.SMALL)

            row("Count:") {
                intTextField(0..1000, 1)
                    .align(AlignX.FILL)
                    .bindIntText(model::countIteration)
                    .comment(
                        "Number of iterations, larger number slows down the process but increases the accuracy",
                        90
                    )
            }
        }

        return mainPanel
    }
}
