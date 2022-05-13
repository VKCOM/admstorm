package com.vk.admstorm.configuration.kbench

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.impl.PhpClassImpl
import com.vk.admstorm.utils.KBenchUtils
import javax.swing.Icon

class KBenchConfigurationLineMarkerProvider : RunLineMarkerContributor() {
    private val contextAction =
        ExecutorAction.getActions(0).firstOrNull { it.toString().startsWith("Run context configuration") }

    override fun getInfo(e: PsiElement): Info? {
        if (e.elementType != PhpTokenTypes.IDENTIFIER) {
            return null
        }

        return when (val parent = e.parent) {
            is PhpClassImpl -> {
                if (!KBenchUtils.isBenchmarkClass(parent)) {
                    return null
                }

                createInfo(AllIcons.RunConfigurations.TestState.Run_run)
            }
            is Method -> {
                if (!KBenchUtils.isBenchmarkMethod(parent)) {
                    return null
                }

                createInfo(AllIcons.RunConfigurations.TestState.Run)
            }
            else -> return null
        }
    }

    private fun createInfo(icon: Icon): Info {
        val actionsGroup = object : ActionGroup("Other", true) {
            override fun getChildren(e: AnActionEvent?): Array<AnAction> {
                return emptyArray()
            }

            override fun update(e: AnActionEvent) {
                // Хак, так как без этой группы не отображаются все
                // виды бенчмарков для запуска.
                e.presentation.isEnabledAndVisible = false
            }
        }

        return Info(icon, { "Run" }, contextAction, actionsGroup)
    }
}
