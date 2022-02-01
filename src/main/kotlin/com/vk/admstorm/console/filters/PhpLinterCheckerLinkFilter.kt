package com.vk.admstorm.console.filters

import com.intellij.execution.filters.Filter
import com.intellij.openapi.project.Project
import com.vk.admstorm.configuration.phplinter.PhpLinterCheckers
import com.vk.admstorm.console.Console

class PhpLinterCheckerLinkFilter(project: Project, private val myConsole: Console) : BasePhpFileLinkFilter(project) {
    /**
     * Parses the warning line by PHP Linter containing the checker name.
     *
     * Examples of warning:
     *
     *    <critical> WARNING codeStructure: Code must only go before function definitions
     *    at script.php:17\n
     *
     *    <critical> <autofixable> ERROR   codeStructure: Code must only go before function definitions
     *    at script.php:17\n
     *
     * @return checker name or null if the line should be skipped
     */
    private fun parseLine(line: String): String? {
        if (!line.startsWith("<critical>")) {
            return null
        }

        val indexFirstColon = line.indexOf(':')
        val beforeColon = line.substring(0 until indexFirstColon)
        val indexLastSpace = beforeColon.lastIndexOf(" ")

        return line.substring(indexLastSpace + 1 until beforeColon.length)
    }

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val checkerName = parseLine(line) ?: return null

        val indexFirstColon = line.indexOf(':')
        val beforeColon = line.substring(0 until indexFirstColon)
        val indexLastSpace = beforeColon.lastIndexOf(" ")

        val startOffset = (entireLength - line.length) + indexLastSpace + 1
        val endOffset = startOffset + checkerName.length

        val description = PhpLinterCheckers.nameToCheckerDoc[checkerName] ?: "no description"

        return Filter.Result(
            startOffset, endOffset
        ) {
            myConsole.showTooltip(description)
        }
    }
}
