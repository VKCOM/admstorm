package com.vk.admstorm.configuration.kbench

enum class KBenchScope {
    Class,
    Method;

    companion object {
        fun from(s: String): KBenchScope = KBenchScope.values().find { it.name == s } ?: Class
    }
}
