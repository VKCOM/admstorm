package com.vk.admstorm.actions

import com.intellij.ide.actions.BigPopupUI
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.BooleanFunction
import com.intellij.util.ui.StartupUiUtil
import com.intellij.util.ui.UIUtil
import com.vk.admstorm.services.RunAnythingOnServerService
import com.vk.admstorm.utils.ServerNameProvider
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class RunAnythingOnServerPopupUI(project: Project?) : BigPopupUI(project) {
    private val mySaveAsConfigurationCheckBox = JBCheckBox("Save as run configuration")

    init {
        init()

        adjustMainListEmptyText(mySearchField)

        val escape = ActionManager.getInstance().getAction("EditorEscape")
        DumbAwareAction.create {
            searchFinishedHandler.run()
        }.registerCustomShortcutSet(escape?.shortcutSet ?: CommonShortcuts.ESCAPE, this)

        DumbAwareAction.create {
            executeCommand()
        }
            .registerCustomShortcutSet(
                CustomShortcutSet.fromString(
                    "ENTER", "shift ENTER", "alt ENTER", "alt shift ENTER", "meta ENTER"
                ),
                this,
                this
            )
    }

    private fun executeCommand() {
        val command = mySearchField.text
        val saveAsConfiguration = mySaveAsConfigurationCheckBox.isSelected

        RunAnythingOnServerService.getInstance(myProject!!).run(command, saveAsConfiguration)

        mySearchField.text = ""
        searchFinishedHandler.run()
    }

    override fun dispose() {}

    override fun createList() = JBList<Any>()

    override fun createCellRenderer() = SimpleListCellRenderer.create<Any>("") { it.toString() }

    override fun createHeader(): JComponent {
        val header = JPanel(BorderLayout())
        header.add(createHeaderLeftPanel(), BorderLayout.WEST)
        header.add(createHeaderSettingsPanel(), BorderLayout.EAST)
        return header
    }

    private fun createHeaderLeftPanel(): JPanel {
        val textFieldTitle = JLabel("Run Anything on ${ServerNameProvider.name()}")
        val topPanel = NonOpaquePanel(BorderLayout())
        val foregroundColor =
            if (StartupUiUtil.isUnderDarcula())
                if (UIUtil.isUnderWin10LookAndFeel()) JBColor.WHITE
                else JBColor(Gray._240, Gray._200)
            else UIUtil.getLabelForeground()

        textFieldTitle.foreground = foregroundColor
        textFieldTitle.border = BorderFactory.createEmptyBorder(5, 5, 5, 0)
        if (SystemInfo.isMac) {
            textFieldTitle.font = textFieldTitle.font.deriveFont(Font.BOLD, textFieldTitle.font.size - 1f)
        } else {
            textFieldTitle.font = textFieldTitle.font.deriveFont(Font.BOLD)
        }

        topPanel.add(textFieldTitle)

        return topPanel
    }

    private fun createHeaderSettingsPanel(): JPanel {
        val topPanel = NonOpaquePanel(BorderLayout())

        mySaveAsConfigurationCheckBox.isOpaque = false
        mySaveAsConfigurationCheckBox.border = BorderFactory.createEmptyBorder(5, 0, 5, 5)

        topPanel.add(mySaveAsConfigurationCheckBox)

        return topPanel
    }

    override fun getAccessibleName() = "Run anything on server"

    private fun adjustMainListEmptyText(editor: JBTextField) {
        adjustEmptyText(editor, "Run command on ${ServerNameProvider.name()}") { field ->
            field.text.isEmpty()
        }
    }

    private fun adjustEmptyText(textEditor: JBTextField, text: String, condition: (JBTextField) -> Boolean) {
        textEditor.putClientProperty("StatusVisibleFunction", BooleanFunction<JBTextField> { condition(textEditor) })
        val statusText = textEditor.emptyText
        statusText.isShowAboveCenter = false
        statusText.setText(text, SimpleTextAttributes.GRAY_ATTRIBUTES)
        statusText.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL))
    }
}
