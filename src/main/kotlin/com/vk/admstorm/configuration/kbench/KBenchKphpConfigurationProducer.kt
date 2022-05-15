package com.vk.admstorm.configuration.kbench

class KBenchKphpConfigurationProducer : KBenchBaseConfigurationProducer() {
    override fun configurationId() = KBenchKphpConfigurationType.ID
}

class KBenchPhpConfigurationProducer : KBenchBaseConfigurationProducer() {
    override fun configurationId() = KBenchPhpConfigurationType.ID
    override fun benchType() = KBenchType.BenchPhp
    override fun namePrefix() = "PHP"
}

class KBenchKphpVsPhpConfigurationProducer : KBenchBaseConfigurationProducer() {
    override fun configurationId() = KBenchKphpVsPhpConfigurationType.ID
    override fun benchType() = KBenchType.BenchVsPhp
    override fun namePrefix() = "PHP vs KPHP"
}

class KBenchKphpAbConfigurationProducer : KBenchBaseConfigurationProducer() {
    override fun configurationId() = KBenchKphpAbConfigurationType.ID
    override fun benchType() = KBenchType.BenchAb
    override fun namePrefix() = "VS Other"
}
