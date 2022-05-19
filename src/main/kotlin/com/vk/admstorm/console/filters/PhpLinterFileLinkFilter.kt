package com.vk.admstorm.console.filters

import com.intellij.execution.filters.Filter
import com.intellij.openapi.project.Project
import com.vk.admstorm.utils.MyEditorUtils
import com.vk.admstorm.utils.MyPathUtils

class PhpLinterFileLinkFilter(project: Project) : BasePhpFileLinkFilter(project) {
    /**
     * Parses the warning line by PHP Linter containing the path to the file.
     *
     * Examples of warning:
     *
     *    <critical> WARNING codeStructure: Code must only go before function definitions
     *    at /home/username/script.php:17\n
     *
     *    <critical> WARNING codeStructure: Code must only go before function definitions
     *    at dir/SomeClass.php:17\n
     *
     * @return Pair(filePath, lineIndex) or null if the line should be skipped
     */
    private fun parseLine(line: String): Pair<String, Int>? {
        if (!line.startsWith("<critical>")) {
            return null
        }

        val indexAfterAt = line.lastIndexOf(" at ") + " at ".length
        val indexLastColon = line.lastIndexOf(':')
        val indexAfterLastColon = indexLastColon + 1

        val filepath = line.substring(indexAfterAt until indexLastColon)
        val stringLineIndex = line.substring(indexAfterLastColon until line.lastIndex)
        val lineIndex = stringLineIndex.toIntOrNull() ?: 0

        return Pair(filepath, lineIndex)
    }

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val (remoteFilepath, lineIndex) = parseLine(line) ?: return null

        val localPath = MyPathUtils.absoluteLocalPathByRemotePath(myProject, remoteFilepath)
            ?: return null

        val startOffset = entireLength - "$remoteFilepath:$lineIndex".length - 1
        val endOffset = entireLength - 1

        return Filter.Result(
            startOffset, endOffset
        ) { project ->
            MyEditorUtils.openFileOnLine(project, localPath, lineIndex)
        }
    }
}
