package com.vk.admstorm.actions.git.listeners

import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.progress.ProgressIndicator
import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class GitProgressListener(indicator: ProgressIndicator) : PatternBasedProgressListener(indicator) {
    abstract fun progressRangeByLabel(label: String): ClosedFloatingPointRange<Double>

    override fun startNotified(event: ProcessEvent) {
        myIndicator.fraction = 0.05
        myIndicator.text2 = "Waiting ssh connection"
    }

    override fun patterns(): List<Pattern> = listOf(
        Pattern.compile("""(remote: )?(?<label>(\w| )+): \d+.*"""),
        Pattern.compile("""(remote: )?(?<label>(\w| )+): (?<percent>\d+)%.*"""),
        Pattern.compile("""(remote: )?(?<label>Total .*)"""),
        Pattern.compile("""(?<label>\d+ file.*)"""),
    )

    override fun onMatch(line: String, matcher: Matcher): Pair<Double, String>? {
        val label = getGroup(matcher, "label") ?: return null

        val percent = getGroup(matcher, "percent") ?: "100"
        val divider = (percent.toIntOrNull() ?: 0) / 100

        val range = progressRangeByLabel(label)
        val shift = (range.endInclusive - range.start) * divider

        return Pair(range.start + shift, label)
    }
}
