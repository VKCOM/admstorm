package com.vk.admstorm.console.filters

import com.intellij.execution.filters.Filter
import com.intellij.openapi.project.Project
import com.vk.admstorm.utils.MyEditorUtils
import com.vk.admstorm.utils.MyPathUtils

class KphpErrorFileLinkFilter(project: Project) : BasePhpFileLinkFilter(project) {
    /**
     * Parses the warning line by KPHP containing the path to the file.
     *
     * Examples:
     *
     *   <space><space>script.php:64  in ClassName::MethodName
     *   <space><space>script.php:20
     *
     * @return Pair(filePath, lineIndex) or null if the line should be skipped
     */
    private fun parseLine(line: String): Pair<String, Int>? {
        if (line.contains("{") || !line.startsWith("  ") || !line.contains(".php")) {
            return null
        }

        val indexColon = line.indexOf(':')
        val indexAfterColon = indexColon + 1

        val filepath = line.substring("  ".length until indexColon)

        val indexIn = line.lastIndexOf("  in")
        val lineIndexEnd = if (indexIn != -1) indexIn else line.length - 1

        val stringLineIndex = line.substring(indexAfterColon until lineIndexEnd)
        val lineIndex = stringLineIndex.toIntOrNull() ?: 0

        return Pair(filepath, lineIndex)
    }

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val (remoteFilepath, lineIndex) = parseLine(line) ?: return null

        val localPath = MyPathUtils.absoluteLocalPathByRemotePath(myProject, remoteFilepath)
            ?: return null

        val indexIn = line.lastIndexOf("  in")
        val reverseIndexIn = line.length - indexIn

        val endOffset = if (indexIn == -1) entireLength - 1 else entireLength - reverseIndexIn
        val startOffset = endOffset - "$remoteFilepath:$lineIndex".length

        return Filter.Result(
            startOffset, endOffset
        ) { project ->
            MyEditorUtils.openFileOnLine(project, localPath, lineIndex)
        }
    }
}
