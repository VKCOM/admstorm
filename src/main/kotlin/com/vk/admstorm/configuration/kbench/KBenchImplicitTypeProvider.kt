package com.vk.admstorm.configuration.kbench

import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpClassMember

class KBenchImplicitTypeProvider : ImplicitUsageProvider {
    override fun isImplicitUsage(element: PsiElement): Boolean {
        if (element is PhpClass && KBenchUtils.isBenchmarkClass(element)) {
            return true
        }

        if (element is PhpClassMember) {
            val klass = element.containingClass ?: return false
            return KBenchUtils.isBenchmarkClass(klass)
        }

        return false
    }

    override fun isImplicitRead(element: PsiElement) = false

    override fun isImplicitWrite(element: PsiElement) = false
}
