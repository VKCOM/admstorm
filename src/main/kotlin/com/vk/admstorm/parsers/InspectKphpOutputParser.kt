package com.vk.admstorm.parsers

object InspectKphpOutputParser {
    private const val startLine = "Source file: "

    fun parse(output: String): String {
        val startFileName = output.indexOf(startLine) + startLine.length
        val endFileName = output.indexOf("\n", startFileName) - 1
        return output.slice(startFileName..endFileName)
    }
}
