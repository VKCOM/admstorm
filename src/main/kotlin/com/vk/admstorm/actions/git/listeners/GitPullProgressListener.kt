package com.vk.admstorm.actions.git.listeners

import com.intellij.openapi.progress.ProgressIndicator

class GitPullProgressListener(indicator: ProgressIndicator) : GitProgressListener(indicator) {
    override fun progressRangeByLabel(label: String): ClosedFloatingPointRange<Double> {
        return when {
            // remote: Total 1 (delta 0), reused 0 (delta 0)
            label.contains("Total ") -> 0.8..0.9
            // 21 files changed, 192 insertions(+), 59 deletions(-)
            label.contains(" file") -> 0.9..0.95
            else -> when (label) {
                "Enumerating objects", "Перечисление объектов" -> 0.1..0.2
                "Counting objects", "Подсчет объектов" -> 0.1..0.2
                "Compressing objects", "Сжатие объектов" -> 0.2..0.4
                "Receiving objects", "Получение объектов" -> 0.4..0.6
                "Resolving deltas", "Определение изменений" -> 0.6..0.8
                else -> 0.0..0.0
            }
        }
    }
}
