package com.vk.admstorm.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor

class WrongBenchmarkNameInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpClass(klass: PhpClass) {
                val namePsi = klass.nameIdentifier ?: return
                val name = klass.name
                if (name.endsWith("Benchmark")) {
                    val withoutBenchmarkSuffix = name.removeSuffix("Benchmark")
                    holder.registerProblem(
                        namePsi,
                        "Perhaps you meant 'Benchmark$withoutBenchmarkSuffix', benchmark class name should be prefixed with 'Benchmark' and not suffixed",
                        ProblemHighlightType.WARNING,
                    )
                }
            }
        }
    }
}
