package com.vk.admstorm.configuration.runanything

import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.fields.ExpandableTextField
import javax.swing.JPanel
import javax.swing.JTextField

open class RunAnythingConfigurationEditor : SettingsEditor<RunAnythingConfiguration>() {
    private var myPanel: JPanel? = null
    private var myParameters: JTextField? = null
    private var myEnvTextField: ExpandableTextField? = null

    override fun resetEditorFrom(demoRunConfiguration: RunAnythingConfiguration) {
        myParameters!!.text = demoRunConfiguration.parameters
        myEnvTextField!!.text = demoRunConfiguration.envVariables
    }

    override fun applyEditorTo(demoRunConfiguration: RunAnythingConfiguration) {
        demoRunConfiguration.parameters = myParameters!!.text ?: ""
        demoRunConfiguration.envVariables = myEnvTextField!!.text ?: ""
    }

    override fun createEditor() = myPanel!!

    init {
        myPanel?.border = IdeBorderFactory.createTitledBorder("Configuration")
    }
}
