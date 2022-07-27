package com.vk.admstorm.utils

import com.intellij.ui.RelativeFont
import com.intellij.ui.dsl.builder.Cell
import java.awt.Font
import javax.swing.JComponent

object UiDslBuilderUtils {
    private val MONOSPACED_FONT = RelativeFont.NORMAL.family(Font.MONOSPACED)

    fun <T : JComponent> Cell<T>.monospace(): Cell<T> {
        MONOSPACED_FONT.install(component)
        return this
    }

    fun <T : JComponent> Cell<T>.italic(): Cell<T> {
        component.font = component.font.deriveFont(Font.ITALIC)
        return this
    }
}
