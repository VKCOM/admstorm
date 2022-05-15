package com.vk.admstorm.configuration.kbench

enum class KBenchType(val command: String, val presentation: String) {
    Bench("bench", "KPHP Benchmark"),
    BenchPhp("bench-php", "PHP Benchmark"),
    BenchVsPhp("bench-vs-php", "KPHP vs PHP Benchmark"),
    BenchAb("bench-ab", "Compare with other Benchmark");

    companion object {
        fun from(s: String): KBenchType = values().find { it.command == s } ?: Bench
    }
}
