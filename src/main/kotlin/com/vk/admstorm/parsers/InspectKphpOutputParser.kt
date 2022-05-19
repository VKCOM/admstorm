package com.vk.admstorm.parsers

object InspectKphpOutputParser {
    private const val START_LINE = "Source file: "

    fun parse(output: String): String {
        val startFileName = output.indexOf(START_LINE) + START_LINE.length
        val endFileName = output.indexOf("\n", startFileName) - 1
        return output.slice(startFileName..endFileName)
    }
}
