package com.vk.admstorm.console.filters

import com.intellij.execution.filters.Filter
import com.intellij.openapi.project.Project

/**
 * Sets the logic for adding clickable text in the console.
 *
 * Each line of the console output first goes to the [applyFilter]
 * method and depending on its result, there may be clickable links
 * in the output line.
 */
abstract class BasePhpFileLinkFilter(protected var myProject: Project) : Filter {
    /**
     * The start and end of the range for [Filter.Result] must be relative
     * to [entireLength] in order for it to work correctly.
     */
    abstract override fun applyFilter(line: String, entireLength: Int): Filter.Result?
}
