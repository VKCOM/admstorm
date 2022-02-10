package com.vk.admstorm.configuration.builders

import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.IdeBorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

open class BuildersConfigurationEditor : SettingsEditor<BuildersConfiguration>() {
    private lateinit var myPanel: JPanel
    private lateinit var myParameters: JTextField

    override fun resetEditorFrom(demoRunConfiguration: BuildersConfiguration) {
        myParameters.text = demoRunConfiguration.parameters
    }

    override fun applyEditorTo(demoRunConfiguration: BuildersConfiguration) {
        demoRunConfiguration.parameters = myParameters.text ?: ""
    }

    override fun createEditor(): JComponent = myPanel

    init {
        myPanel.border = IdeBorderFactory.createTitledBorder("Configuration")
    }
}
