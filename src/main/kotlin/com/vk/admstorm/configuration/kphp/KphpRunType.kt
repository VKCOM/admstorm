package com.vk.admstorm.configuration.kphp

enum class KphpRunType(
    var command: String = "",
    var arguments: String = "",
    var description: String = ""
) {
    Bu, Ru, Sc,
    No, Ta, Jl,
    Co, Ge, Pr,
    Mi, Mis, My;

    fun getShowText() = when {
        command.isEmpty() -> "<unknown #$ordinal>"
        description.isEmpty() -> command
        else -> "$command — $description"
    }

    companion object {
        fun from(s: String): KphpRunType = values().find { it.name == s } ?: Bu
    }
}
