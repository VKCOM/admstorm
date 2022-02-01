package com.vk.admstorm.configuration.phplinter

import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

open class PhpLinterConfigurationEditor : SettingsEditor<PhpLinterConfiguration>() {
    private lateinit var myPanel: JPanel
    private lateinit var myParameters: JTextField
    private lateinit var myRunAsInTeamcityCheckBox: JBCheckBox

    override fun resetEditorFrom(demoRunConfiguration: PhpLinterConfiguration) {
        myParameters.text = demoRunConfiguration.parameters
        myRunAsInTeamcityCheckBox.isSelected = demoRunConfiguration.runAsInTeamcity
    }

    override fun applyEditorTo(demoRunConfiguration: PhpLinterConfiguration) {
        demoRunConfiguration.parameters = myParameters.text ?: ""
        demoRunConfiguration.runAsInTeamcity = myRunAsInTeamcityCheckBox.isSelected
    }

    override fun createEditor(): JComponent = myPanel

    init {
        myPanel.border = IdeBorderFactory.createTitledBorder("Configuration")
    }
}
