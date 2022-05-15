package com.vk.admstorm.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor

class WrongFileNameBenchmarkInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpClass(klass: PhpClass) {
                val namePsi = klass.nameIdentifier ?: return
                val name = klass.name
                if (!name.startsWith("Benchmark")) {
                    return
                }

                val containingFile = klass.containingFile
                val filename = containingFile.name

                if (!filename.startsWith("$name.")) {
                    holder.registerProblem(
                        namePsi,
                        "Filename '$filename' does not match the class name '$name' of the benchmark",
                        ProblemHighlightType.WARNING,
                    )
                }
            }
        }
    }
}
