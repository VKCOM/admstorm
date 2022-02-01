package com.vk.admstorm.parsers

import com.intellij.openapi.project.Project
import com.vk.admstorm.configuration.problems.panels.Problem
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MyUtils

object PhpLinterWarningsParser {
    fun parse(project: Project, output: String): List<Problem> {
        val lines = output.split("\n")
        val warningIndexes = lines.mapIndexedNotNull { index, line ->
            if (line.contains("<critical>"))
                index
            else
                null
        }

        return warningIndexes.map { warningIndex ->
            val head = lines[warningIndex]
                .removePrefix("<critical> ")
                .removePrefix("<autofixable> ")

            val firstSpaceIndex = head.indexOf(' ')
            val kind = when (head.substring(0 until firstSpaceIndex)) {
                "ERROR" -> Problem.Kind.Error
                "WARNING" -> Problem.Kind.Warning
                "MAYBE" -> Problem.Kind.Info
                else -> Problem.Kind.Warning
            }

            val firstColonIndex = head.indexOf(':')
            val name = head.substring(firstSpaceIndex until firstColonIndex).trim()

            val lastColonIndex = head.lastIndexOf(':')
            val line = head.substring(lastColonIndex + 1).toIntOrNull() ?: -1

            val atIndex = head.lastIndexOf(" at")
            val description = head.substring(firstColonIndex + 2 until atIndex)
            val file = head.substring(atIndex until lastColonIndex).removePrefix(" at ")

            val absPath = MyPathUtils.absoluteLocalPathByRemotePath(project, file) ?: file
            val virtualFile = MyUtils.virtualFileByName(absPath)

            Problem(name, virtualFile, absPath, line, description, kind)
        }
    }
}