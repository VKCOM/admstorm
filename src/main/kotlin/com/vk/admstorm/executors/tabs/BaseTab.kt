package com.vk.admstorm.executors.tabs

import com.intellij.execution.ui.ExecutionConsole
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComponentWithActions
import com.intellij.ui.content.Content
import javax.swing.Icon
import javax.swing.JComponent

abstract class BaseTab(private var myName: String) : Tab {
    companion object {
        private var myCounter = 2
            get() = field++
    }

    private var myId = myCounter
    private var myContent: Content? = null
    private var myLayout: RunnerLayoutUi? = null

    override fun getName() = myName
    override fun getContent() = myContent

    abstract fun componentWithActions(): ComponentWithActions
    abstract fun componentToFocus(): JComponent

    open fun afterAdd() {}
    open fun icon(): Icon = AllIcons.Debugger.Console

    override fun addTo(layout: RunnerLayoutUi) {
        myLayout = layout

        myContent = layout.createContent(
            ExecutionConsole.CONSOLE_CONTENT_ID + myName.replace(" ", "_"),
            componentWithActions(),
            myName,
            icon(),
            componentToFocus()
        )

        myContent!!.description = myName

        myLayout?.addContent(myContent!!, myId, PlaceInGrid.right, false)
        afterAdd()
    }
}
