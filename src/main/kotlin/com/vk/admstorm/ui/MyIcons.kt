package com.vk.admstorm.ui

import com.intellij.ui.IconManager

object MyIcons {
    private fun icon(name: String) = IconManager.getInstance().getIcon(name, javaClass)

    val kphp = icon("/icons/kphp.svg")
    val kphpBench = icon("/icons/kphp-bench.svg")
    val phpLinter = icon("/icons/php-linter.svg")
    val yarn = icon("/icons/yarn")
    val yarnWatchWorking = icon("/icons/yarn-working")
    val yarnWatchError = icon("/icons/yarn-error")
    val yarnWatchStopped = icon("/icons/yarn-disabled")
    val sentry = icon("/icons/sentry")
}
