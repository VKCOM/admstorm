package com.vk.admstorm.executors.tabs

import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.ui.content.Content

interface Tab {
    fun addTo(layout: RunnerLayoutUi)
    fun getName(): String
    fun getContent(): Content?
}
