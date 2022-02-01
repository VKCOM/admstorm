package com.vk.admstorm.configuration.kbench

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.IdeBorderFactory
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

open class KBenchConfigurationEditor : SettingsEditor<KBenchConfiguration>() {
    private var myPanel: JPanel? = null
    private var myScriptName: TextFieldWithBrowseButton? = null
    private var myBenchType: ComboBox<String>? = null
    private var myCountRuns: JTextField? = null

    override fun resetEditorFrom(demoRunConfiguration: KBenchConfiguration) {
        myScriptName!!.text = demoRunConfiguration.scriptName
        myBenchType!!.selectedItem = demoRunConfiguration.benchType.command
        myCountRuns!!.text = demoRunConfiguration.countRuns
    }

    override fun applyEditorTo(demoRunConfiguration: KBenchConfiguration) {
        demoRunConfiguration.scriptName = myScriptName!!.text
        demoRunConfiguration.benchType = KBenchType.from(myBenchType!!.selectedItem as String)
        demoRunConfiguration.countRuns = myCountRuns!!.text
    }

    override fun createEditor(): JComponent = myPanel!!

    init {
        myScriptName?.addBrowseFolderListener(
            "Select PHP Bench File or Folder", "Select PHP bench file or folder to run", null,
            FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor()
        )

        val boxModel = DefaultComboBoxModel<String>()
        listOf("bench", "bench-php", "bench-vs-php").forEach {
            boxModel.addElement(it)
        }

        myBenchType?.model = boxModel
        myBenchType?.selectedItem = "bench"

        myPanel?.border = IdeBorderFactory.createTitledBorder("Configuration")
    }
}
