package com.vk.admstorm.configuration.problems.panels

import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

data class Problem(
    val name: String?,
    val file: VirtualFile?,
    val path: String,
    val line: Int,
    val description: String,
    val kind: Kind,
) {
    val icon: Icon
        get() = when (kind) {
            Kind.Error -> HighlightDisplayLevel.ERROR.icon
            Kind.Warning -> HighlightDisplayLevel.WARNING.icon
            Kind.Info -> HighlightDisplayLevel.WEAK_WARNING.icon
        }

    enum class Kind {
        Error,
        Warning,
        Info,
    }
}
