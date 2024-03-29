package com.vk.admstorm.highlight.markers.impl

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.Label
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.util.ui.JBUI
import com.vk.admstorm.admscript.AdmScript
import com.vk.admstorm.utils.UiDslBuilderUtils.italic
import org.apache.commons.lang.StringEscapeUtils
import org.jetbrains.annotations.Nls
import javax.swing.*

abstract class MarkerService<TModel>(project: Project) : AdmScript<TModel>(project), IMarkerPopup<TModel> {
    protected fun popupPanel(init: Panel.() -> Unit): DialogPanel {
        val topBottom = 10
        val leftRight = 15

        return panel {
            panel {
                customize(UnscaledGaps(topBottom, leftRight, topBottom, leftRight))

                init()
            }
        }
    }

    protected fun Panel.rowTitle(@Nls text: String, isDangerous: Boolean = false, init: Row.() -> Unit): Row {
        return row(Label(text, bold = true)) {
            init()

            if (isDangerous) {
                label("(опасная)")
                    .bold()
            }
        }
    }

    protected fun Panel.rowContent(@Nls text: String, init: Row.() -> Unit): Row {
        val label = Label(text).apply {
            foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        }

        return row(label) {
            init()
        }
    }

    protected fun Row.date(@Nls text: String): Cell<JLabel> {
        return label(text)
            .italic()
    }

    protected fun Row.description(@Nls text: String): Cell<JEditorPane> {
        return text(text, 90)
    }

    enum class ViewMode {
        SINGLE,
        MULTILINE;

        companion object {
            fun getByName(viewMode: String): ViewMode {
                return ViewMode.values().firstOrNull { it.name.lowercase() == viewMode } ?: SINGLE
            }
        }
    }

    fun Row.textComponent(text: String, viewMode: ViewMode): Cell<JComponent> {
        return cell(renderTextComponent(text, viewMode))
    }

    private fun renderTextComponent(text: String, viewMode: ViewMode): JComponent {
        val unescapeText = StringEscapeUtils.unescapeJavaScript(text)

        return when (viewMode) {
            ViewMode.SINGLE -> {
                return JLabel(unescapeText)
            }

            ViewMode.MULTILINE -> {
                val textArea = JBTextArea().apply {
                    isEditable = false
                    rows = 10
                    columns = 45
                }
                textArea.text = unescapeText

                JBScrollPane(textArea).apply {
                    verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
                }.apply {
                    border = BorderFactory.createEmptyBorder()
                }
            }
        }
    }

    fun errorMessage(textError: String): JComponent {
        val topBottom = 2
        val leftRight = 5

        val internalPanel = panel {
            row {
                icon(AllIcons.General.NotificationError)
                    .gap(RightGap.SMALL)

                text(textError)
            }
        }.apply {
            background = JBUI.CurrentTheme.NotificationError.backgroundColor()
            foreground = JBUI.CurrentTheme.NotificationError.foregroundColor()
            border = BorderFactory.createEmptyBorder(topBottom, leftRight, topBottom, leftRight)
        }

        return panel {
            row {
                cell(internalPanel)
                    .align(AlignX.FILL)
            }
        }.apply {
            border = JBUI.Borders.customLine(
                JBUI.CurrentTheme.Popup.borderColor(true),
                topBottom + 1,
                leftRight + 1,
                topBottom + 1,
                leftRight + 1,
            )
        }
    }

    fun loader(): JComponent {
        val topBottom = 2
        val leftRight = 5

        val internalPanel = panel {
            row {
                icon(AnimatedIcon.Default())
                    .gap(RightGap.SMALL)

                label("Загрузка, работяга")
            }
        }.apply {
            border = BorderFactory.createEmptyBorder(topBottom, leftRight, topBottom, leftRight)
        }

        return panel {
            row {
                cell(internalPanel)
                    .align(AlignX.FILL)
            }
        }
    }
}
