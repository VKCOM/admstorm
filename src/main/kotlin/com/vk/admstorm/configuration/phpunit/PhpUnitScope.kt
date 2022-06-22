package com.vk.admstorm.configuration.phpunit

enum class PhpUnitScope {
    Directory,
    Class,
    Method;

    companion object {
        fun from(s: String): PhpUnitScope = PhpUnitScope.values().find { it.name == s } ?: Directory
    }
}
