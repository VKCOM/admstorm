package com.vk.admstorm.configuration.problems.panels

import com.intellij.pom.Navigatable

interface NavigatableTreeNode {
    fun getNavigatable(): Navigatable?
}
