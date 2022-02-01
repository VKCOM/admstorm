package com.vk.admstorm.configuration.kphp

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.fields.ExpandableTextField
import javax.swing.DefaultComboBoxModel
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

open class KphpConfigurationEditor : SettingsEditor<KphpConfiguration>() {
    private var myPanel: JPanel? = null
    private var myRunType: ComboBox<KphpRunType>? = null
    private var myParameters: JTextField? = null
    private var myCommandsArgs: JLabel? = null
    private var myEnvTextField: ExpandableTextField? = null
    private var myRunScriptWithPhp: JBCheckBox? = null

    override fun resetEditorFrom(demoRunConfiguration: KphpConfiguration) {
        myRunType!!.selectedItem = demoRunConfiguration.runType
        myParameters!!.text = demoRunConfiguration.parameters
        myRunScriptWithPhp!!.isSelected = demoRunConfiguration.runScriptWithPhp
        myEnvTextField!!.text = demoRunConfiguration.envVariables
    }

    override fun applyEditorTo(demoRunConfiguration: KphpConfiguration) {
        demoRunConfiguration.runType = myRunType!!.selectedItem as KphpRunType
        demoRunConfiguration.parameters = myParameters!!.text ?: ""
        demoRunConfiguration.runScriptWithPhp = myRunScriptWithPhp!!.isSelected
        demoRunConfiguration.envVariables = myEnvTextField!!.text ?: ""
    }

    override fun createEditor() = myPanel!!

    init {
        myRunScriptWithPhp!!.isVisible = false

        myRunType?.addItemListener { e ->
            val selected = e.itemSelectable.selectedObjects[0] as KphpRunType
            myCommandsArgs?.text = selected.arguments

            myRunScriptWithPhp!!.isVisible = selected == KphpRunType.Sc
        }

        val boxModel = DefaultComboBoxModel<KphpRunType>()
        KphpRunType.values().forEach {
            boxModel.addElement(it)
        }

        myRunType?.renderer = SimpleListCellRenderer.create("", KphpRunType::getShowText)
        myRunType?.model = boxModel

        myCommandsArgs?.text = (myRunType?.selectedItem as KphpRunType).arguments

        myPanel?.border = IdeBorderFactory.createTitledBorder("Configuration")
    }
}
