package com.vk.admstorm.actions.git.panels

import com.intellij.ui.ColoredTreeCellRenderer

interface AdmRenderedTreeNode {
    fun render(renderer: ColoredTreeCellRenderer)
}
