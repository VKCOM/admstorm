package com.vk.admstorm.utils

import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass

object KBenchUtils {
    fun isBenchmarkClass(klass: PhpClass) = klass.name.startsWith("Benchmark")
    fun isBenchmarkMethod(method: Method) = method.name.startsWith("benchmark")
}
