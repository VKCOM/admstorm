package com.vk.admstorm.ui

import com.intellij.ui.IconManager

object MyIcons {
    val kphp = IconManager.getInstance().getIcon("/icons/kphp.svg", javaClass)
    val kphpBench = IconManager.getInstance().getIcon("/icons/kphp-bench.svg", javaClass)
    val phpLinter = IconManager.getInstance().getIcon("/icons/php-linter.svg", javaClass)
    val yarn = IconManager.getInstance().getIcon("/icons/yarn", javaClass)
    val yarnWatchWorking = IconManager.getInstance().getIcon("/icons/yarn-working", javaClass)
    val yarnWatchError = IconManager.getInstance().getIcon("/icons/yarn-error", javaClass)
    val yarnWatchStopped = IconManager.getInstance().getIcon("/icons/yarn-disabled", javaClass)
}
