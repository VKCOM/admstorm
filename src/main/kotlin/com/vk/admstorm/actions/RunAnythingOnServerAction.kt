package com.vk.admstorm.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.WindowStateService
import com.intellij.util.ui.JBInsets
import java.awt.Dimension

class RunAnythingOnServerAction : AdmActionBase() {
    companion object {
        private const val LOCATION_SETTINGS_KEY = "run.anything.on.server.popup"
    }

    override fun actionWithConnectionPerformed(e: AnActionEvent) {
        val view = RunAnythingOnServerPopupUI(e.project!!)

        val balloon =
            JBPopupFactory.getInstance().createComponentPopupBuilder(view, view.searchField)
                .setProject(e.project!!)
                .setModalContext(false)
                .setCancelOnClickOutside(true)
                .setRequestFocus(true)
                .setCancelKeyEnabled(false)
                .addUserData("SIMPLE_WINDOW")
                .setResizable(true)
                .setMovable(true)
                .setDimensionServiceKey(e.project!!, LOCATION_SETTINGS_KEY, true)
                .setLocateWithinScreenBounds(false)
                .createPopup()

        val minSize = view.minimumSize
        JBInsets.addTo(minSize, balloon.content.insets)
        balloon.setMinimumSize(minSize)
        saveSize(e.project!!, minSize)

        balloon.pack(false, true)

        view.setSearchFinishedHandler {
            if (!balloon.isDisposed) {
                balloon.cancel()
            }
        }

        balloon.showInFocusCenter()
    }

    private fun saveSize(project: Project, size: Dimension) {
        WindowStateService.getInstance(project)
            .putSize(LOCATION_SETTINGS_KEY, size)
    }
}
