package com.vk.admstorm.executors.tabs

import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.ui.content.Content
import com.vk.admstorm.executors.BaseRemoteExecutor

/**
 * Base class for all tabs for the [BaseRemoteExecutor].
 */
interface Tab {
    val name: String
    val content: Content?

    /**
     * Adds a tab to the passed UI.
     *
     * @param layout UI to add the tab to
     */
    fun addAsContentTo(layout: RunnerLayoutUi)

    /**
     * Clears the tab's content.
     */
    fun clear()
}
