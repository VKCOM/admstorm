package com.vk.admstorm.ui

import com.intellij.ui.IconManager

object MyIcons {
    private fun icon(name: String) = IconManager.getInstance().getIcon(name, javaClass)

    val kphp = icon("/icons/kphp.svg")
    val kphpBench = icon("/icons/kphp-bench.svg")
    val phpLinter = icon("/icons/php-linter.svg")
    val yarn = icon("/icons/yarn")
    val toolWorking = icon("/icons/tool-working")
    val toolError = icon("/icons/tool-error")
    val toolStopped = icon("/icons/tool-stopped")
    val sentry = icon("/icons/sentry")
    val logs = icon("/icons/show-logs")
}
