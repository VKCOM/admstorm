package com.vk.admstorm.console

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.IdeTooltip
import com.intellij.ide.IdeTooltipManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.panels.NonOpaquePanel
import com.vk.admstorm.console.filters.KphpErrorFileLinkFilter
import com.vk.admstorm.console.filters.PhpLinterCheckerLinkFilter
import com.vk.admstorm.console.filters.PhpLinterFileLinkFilter
import com.vk.admstorm.utils.MyUiUtils
import java.awt.FlowLayout
import java.awt.Point
import javax.swing.border.EmptyBorder

/**
 * A console component with the ability to get the displayed text,
 * as well as applying some standard filters for the output.
 */
class Console(project: Project, withFilters: Boolean = true) {
    private var myConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console

    fun component() = myConsoleView.component
    fun view() = myConsoleView
    fun clear() = myConsoleView.clear()

    fun println(text: String = "", outputType: ConsoleViewContentType = ConsoleViewContentType.NORMAL_OUTPUT) {
        print("$text\n", outputType)
    }

    fun printlnError(text: String) {
        println(text, ConsoleViewContentType.ERROR_OUTPUT)
    }

    fun printlnSystem(text: String) {
        println(text, ConsoleViewContentType.SYSTEM_OUTPUT)
    }

    private fun print(text: String, outputType: ConsoleViewContentType = ConsoleViewContentType.NORMAL_OUTPUT) {
        myConsoleView.print(text, outputType)
    }

    /**
     * Show a tooltip with the passed [text] in the upper left corner of the console.
     * This tooltip will automatically disappear if you move the mouse.
     * In text, you can use HTML to mark up elements.
     */
    fun showTooltip(text: String) {
        val panel = NonOpaquePanel(FlowLayout(FlowLayout.CENTER, 0, 0))
        panel.border = EmptyBorder(10, 10, 10, 10)
        panel.add(MyUiUtils.createTextInfoComponent(text))

        val tooltip = object : IdeTooltip(component(), Point(0, 0), panel) {
            override fun canBeDismissedOnTimeout() = false
        }

        IdeTooltipManager.getInstance().show(tooltip, true)
    }

    init {
        if (withFilters) {
            myConsoleView.addMessageFilter(KphpErrorFileLinkFilter(project))
            myConsoleView.addMessageFilter(PhpLinterFileLinkFilter(project))
            myConsoleView.addMessageFilter(PhpLinterCheckerLinkFilter(project, this))
        }
    }
}
