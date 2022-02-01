package com.vk.admstorm.configuration.kbench

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.impl.PhpClassImpl

class KBenchConfigurationLineMarkerProvider : RunLineMarkerContributor() {
    private val allActions =
        ExecutorAction.getActions(0).filter { it.toString().startsWith("Run context configuration") }.toTypedArray()

    override fun getInfo(e: PsiElement): Info? {
        if (e !is PhpClassImpl) {
            return null
        }
        if (!e.fqn.contains("Benchmark")) {
            return null
        }

        val actions = allActions
        return object : Info(
            AllIcons.RunConfigurations.TestState.Run_run, { element1 ->
                actions.mapNotNull { action ->
                    getText(action, element1)
                }.joinToString("\n")
            },
            *actions
        ) {
            override fun shouldReplace(other: Info) = true
        }
    }
}
