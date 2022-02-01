package com.vk.admstorm.utils

import com.intellij.openapi.fileTypes.ex.FileTypeChooser
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.ColorUtil
import com.intellij.ui.LayeredIcon
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Insets
import javax.accessibility.AccessibleContext
import javax.swing.Icon
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.plaf.basic.BasicHTML
import javax.swing.text.DefaultStyledDocument

object MyUiUtils {
    fun createTextInfoComponent(message: String): JTextPane {
        val component = JTextPane(DefaultStyledDocument())
        component.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, java.lang.Boolean.TRUE)
        component.contentType = "text/html"
        component.isOpaque = false
        component.isFocusable = false
        component.border = null

        val kit = UIUtil.JBWordWrapHtmlEditorKit()
        kit.styleSheet.addRule("a {color: " + ColorUtil.toHtmlColor(JBUI.CurrentTheme.Link.Foreground.ENABLED) + "}")
        component.editorKit = kit
        component.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)

        if (BasicHTML.isHTMLString(message)) {
            component.putClientProperty(
                AccessibleContext.ACCESSIBLE_NAME_PROPERTY,
                StringUtil.unescapeXmlEntities(StringUtil.stripHtml(message, " "))
            )
        }

        component.text = message

        component.isEditable = false
        if (component.caret != null) {
            component.caretPosition = 0
        }

        return component
    }

    fun spacer(width: Int): JPanel {
        return JPanel().apply {
            layout = GridLayoutManager(
                1, 1,
                Insets(0, width, 0, 0), -1, -1
            )
        }
    }

    fun fileTypeIcon(filename: String) =
        createLayeredIcon(FileTypeChooser.getKnownFileTypeOrAssociate(filename)?.icon)

    fun createLayeredIcon(icon: Icon?, vShift: Int = 0): LayeredIcon {
        val emptyFileTypeIcon = EmptyIcon.ICON_18

        val layeredIcon = LayeredIcon(2)
        layeredIcon.setIcon(emptyFileTypeIcon, 0)

        if (icon != null) {
            layeredIcon.setIcon(
                icon,
                1,
                (-icon.iconWidth + emptyFileTypeIcon.iconWidth) / 2,
                (emptyFileTypeIcon.iconHeight - icon.iconHeight) / 2 + vShift
            )
        }

        return layeredIcon
    }
}
