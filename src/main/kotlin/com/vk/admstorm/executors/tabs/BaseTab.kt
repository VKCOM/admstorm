package com.vk.admstorm.executors.tabs

import com.intellij.execution.ui.ExecutionConsole
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComponentWithActions
import com.intellij.ui.content.Content
import javax.swing.Icon
import javax.swing.JComponent

abstract class BaseTab(private var myName: String) : Tab {
    private var myContent: Content? = null

    override val name = myName
    override val content get() = myContent

    abstract fun componentWithActions(): ComponentWithActions
    abstract fun componentToFocus(): JComponent

    open fun afterAdd() {}
    open fun icon(): Icon = AllIcons.Debugger.Console

    override fun addAsContentTo(layout: RunnerLayoutUi) {
        val content = layout.createContent(
            ExecutionConsole.CONSOLE_CONTENT_ID + myName.replace(" ", "_"),
            componentWithActions(),
            myName,
            icon(),
            componentToFocus()
        )

        content.description = myName
        content.isCloseable = false

        layout.addContent(content)

        myContent = content

        afterAdd()
    }
}
