package com.vk.admstorm.actions.git.listeners

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.Key
import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class PatternBasedProgressListener(protected val myIndicator: ProgressIndicator) : ProcessListener {
    companion object {
        private val LOG = logger<PatternBasedProgressListener>()
    }

    override fun startNotified(event: ProcessEvent) {
    }

    override fun processTerminated(event: ProcessEvent) {
        myIndicator.fraction = 1.0
        myIndicator.text2 = "Complete"
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val line = event.text
        LOG.info("line: ${line.replace("\n", " ")}")

        patterns().forEach { pattern ->
            val matcher = pattern.matcher(line)
            if (!matcher.find()) {
                return@forEach
            }

            val res = onMatch(line, matcher) ?: return@forEach
            val (percent, text) = res
            myIndicator.fraction = percent
            myIndicator.text2 = text
        }
    }

    /**
     * Helper function for getting the group with name [groupName] for the passed [matcher].
     * @return null if no such group exists
     */
    protected fun getGroup(matcher: Matcher, groupName: String): String? = try {
        matcher.group(groupName)
    } catch (e: Exception) {
        LOG.warn("Failed to get group $groupName", e)
        null
    }

    open fun patterns(): List<Pattern> = emptyList()

    abstract fun onMatch(line: String, matcher: Matcher): Pair<Double, String>?
}
