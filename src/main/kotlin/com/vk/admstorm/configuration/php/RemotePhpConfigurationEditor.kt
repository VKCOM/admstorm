package com.vk.admstorm.configuration.php

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.IdeBorderFactory
import com.jetbrains.php.lang.PhpFileType
import javax.swing.JComponent
import javax.swing.JPanel

open class RemotePhpConfigurationEditor : SettingsEditor<RemotePhpConfiguration>() {
    private lateinit var myPanel: JPanel
    private lateinit var myScriptName: TextFieldWithBrowseButton

    override fun resetEditorFrom(demoRunConfiguration: RemotePhpConfiguration) {
        myScriptName.text = demoRunConfiguration.scriptName
    }

    override fun applyEditorTo(demoRunConfiguration: RemotePhpConfiguration) {
        demoRunConfiguration.scriptName = myScriptName.text
    }

    override fun createEditor(): JComponent = myPanel

    init {
        myScriptName.addBrowseFolderListener(
            "Select PHP File", "Select PHP file to run", null,
            FileChooserDescriptorFactory.createSingleFileDescriptor(PhpFileType.INSTANCE)
        )

        myPanel.border = IdeBorderFactory.createTitledBorder("Configuration")
    }
}
