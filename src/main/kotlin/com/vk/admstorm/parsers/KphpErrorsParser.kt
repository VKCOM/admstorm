package com.vk.admstorm.parsers

import com.intellij.openapi.project.Project
import com.vk.admstorm.configuration.problems.panels.Problem
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MyUtils

object KphpErrorsParser {
    fun parse(project: Project, output: String): List<Problem> {
        val lines = output.split("\n")
        val errorIndexes = lines.mapIndexedNotNull { index, line ->
            if (line.startsWith("Compilation error at stage: "))
                index
            else
                null
        }

        return errorIndexes.map { errorIndex ->
            val head = lines[errorIndex]
                .removePrefix("Compilation error at stage: ")
            val pathLine = lines[errorIndex + 1]
            val description = lines[errorIndex + 4]

            val genByLabelIndex = head.indexOf(", gen by")
            val stage = head.substring(0 until genByLabelIndex)

            val indexColon = pathLine.indexOf(':')
            val indexAfterColon = indexColon + 1

            val file = pathLine.substring("  ".length until indexColon)

            val indexIn = pathLine.lastIndexOf("  in")
            val lineIndexEnd = if (indexIn != -1) indexIn else pathLine.length - 1

            val stringLineIndex = pathLine.substring(indexAfterColon until lineIndexEnd)
            val line = stringLineIndex.toIntOrNull() ?: 0

            // Always error.
            val kind = Problem.Kind.Error

            val absPath = MyPathUtils.absoluteLocalPathByRemotePath(project, file) ?: file
            val virtualFile = MyUtils.virtualFileByName(absPath)

            Problem(stage, virtualFile, absPath, line, description, kind)
        }
    }
}
