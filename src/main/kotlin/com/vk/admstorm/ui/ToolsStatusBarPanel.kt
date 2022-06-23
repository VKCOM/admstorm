package com.vk.admstorm.ui

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.wm.impl.status.TextPanel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel

class ToolsStatusBarPanel : JPanel() {
    private val label = object : TextPanel() {}
    private val iconLabel = JLabel("")

    init {
        isOpaque = false
        layout = BoxLayout(this, BoxLayout.LINE_AXIS)
        border = JBUI.Borders.empty()
        alignmentY = CENTER_ALIGNMENT

        label.font = if (SystemInfo.isMac) JBUI.Fonts.label(11f) else JBFont.label()
        add(label)

        iconLabel.border = JBUI.Borders.empty(2, 2, 2, 0)
        add(iconLabel)
    }

    fun setText(text: String) {
        label.text = text
    }

    val text: String?
        get() = label.text

    fun setIcon(icon: Icon?) {
        iconLabel.icon = icon
        iconLabel.isVisible = icon != null
    }
}
