package com.vk.admstorm.configuration.kbench

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.util.TextFieldCompletionProvider
import com.intellij.util.textCompletion.TextFieldWithCompletion
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.PhpIcons
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass

object KBenchUtils {
    fun isBenchmarkClass(klass: PhpClass) = isBenchmarkClass(klass.name)
    fun isBenchmarkClass(name: String) = name.startsWith("Benchmark")
    fun isBenchmarkMethod(method: Method) = method.name.startsWith("benchmark")
    fun benchmarkName(name: String) = name.removePrefix("benchmark").removePrefix("_")
    fun className(name: String) = name.split('\\').last()

    fun createMethodCompletionTextField(
        project: Project,
        className: () -> String,
        condition: (Method) -> Boolean = { true },
    ): TextFieldWithCompletion {
        return TextFieldWithCompletion(
            project,
            MethodTextFieldCompletionProvider(project, className, condition),
            "", true, true, true
        )
    }

    private class MethodTextFieldCompletionProvider(
        private val project: Project,
        private val className: () -> String,
        private val condition: (Method) -> Boolean,
    ) : TextFieldCompletionProvider(), DumbAware {
        override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {
            val selectedClass = PhpIndex.getInstance(project).getAnyByFQN(className()).firstOrNull()

            if (selectedClass != null) {
                PhpClassHierarchyUtils.processMethods(
                    selectedClass,
                    selectedClass,
                    { method: Method, _: PhpClass, _: PhpClass ->
                        if (isBenchmarkMethod(method) && condition(method)) {
                            result.addElement(LookupElementBuilder.create(method).withIcon(PhpIcons.PHP_TEST_METHOD))
                        }
                        true
                    },
                    false,
                    false
                )
            }
        }
    }
}
