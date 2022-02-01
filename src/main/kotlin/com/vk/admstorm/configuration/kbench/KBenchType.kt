package com.vk.admstorm.configuration.kbench

enum class KBenchType(val command: String) {
    Bench("bench"),
    BenchPhp("bench-php"),
    BenchVsPhp("bench-vs-php");

    companion object {
        fun from(s: String): KBenchType = values().find { it.command == s } ?: Bench
    }
}
