package com.vk.admstorm.configuration.yarnwatch

import com.intellij.openapi.options.SettingsEditor
import com.intellij.util.ui.JBUI
import javax.swing.JComponent

class YarnWatchConfigurationEditor : SettingsEditor<YarnWatchConfiguration>() {
    override fun createEditor(): JComponent = JBUI.Panels.simplePanel()
    override fun resetEditorFrom(demoRunConfiguration: YarnWatchConfiguration) {}
    override fun applyEditorTo(demoRunConfiguration: YarnWatchConfiguration) {}
}
