package com.vk.admstorm.executors

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.project.Project
import com.vk.admstorm.console.Console
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

abstract class ActionToolbarPanel(protected var project: Project, id: String) : Disposable {
    protected var myActionGroup = DefaultActionGroup()
    protected var myActionGroupToolBar = ActionToolbarImpl("AdmStormToolPanel$id", myActionGroup, false)
    protected var myConsole: Console = Console(project)
    protected var myToolbarComponent: JComponent

    init {
        val actionGroup = ActionManager.getInstance().createActionToolbar("AdmStormToolBar$id", myActionGroup, false)
        actionGroup.targetComponent = myConsole.component()

        val panel = JPanel()
        panel.layout = BorderLayout()
        panel.add(myConsole.component(), BorderLayout.CENTER)
        panel.add(actionGroup.component, BorderLayout.WEST)

        myToolbarComponent = panel
    }

    override fun dispose() {
        myConsole.dispose()
    }
}
