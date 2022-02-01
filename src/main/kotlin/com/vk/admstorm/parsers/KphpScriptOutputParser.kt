package com.vk.admstorm.parsers

import com.intellij.execution.Output

object KphpScriptOutputParser {
    private const val isAlreadyRunningKphpError = "KPHP already started by you"

    private fun isAlreadyStartedKphpError(output: String): Boolean {
        return output.contains(isAlreadyRunningKphpError)
    }

    fun parse(output: Output): String {
        if (output.exitCode == 0) {
            return ""
        }

        if (isAlreadyStartedKphpError(output.stdout)) {
            return output.stdout.trim()
        }

        val compilationErrors = output.stdout

        return compilationErrors
            .replace("\n\n~~", "~~")
            .trim()
    }
}
