package com.vk.admstorm.highlight

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.jetbrains.php.lang.parser.PhpElementTypes
import com.jetbrains.php.lang.psi.elements.ConcatenationExpression
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.vk.admstorm.env.Env
import com.vk.admstorm.utils.MyUtils

class DebugLogLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        call: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>?>
    ) {
        if (call !is FunctionReference) {
            return
        }

        val func = call.resolve() as? Function ?: return
        val fqn = func.fqn.replace(".", "::")
        if (!Env.data.debugLogFqns.contains(fqn) || call.parameters.size < 2) {
            return
        }

        val prefixPsi = call.parameterList?.getParameter(0)

        val (prefix, needInput, inputExpression) = logNameParts(prefixPsi)
        if (prefix == null) {
            return
        }

        val sizePsi = call.parameterList?.getParameter(1) ?: return
        if (sizePsi.elementType != PhpElementTypes.NUMBER) return

        val size = sizePsi.text

        result.add(OpenUrlLineMarkerInfo(prefix, size, needInput, inputExpression?.text, call))
    }

    private fun logNameParts(prefixPsi: PsiElement?): Triple<String?, Boolean, PsiElement?> {
        if (prefixPsi == null) {
            return Triple(null, false, null)
        }

        if (prefixPsi is StringLiteralExpression) {
            return Triple(prefixPsi.contents, false, null)
        }

        if (prefixPsi !is ConcatenationExpression) {
            return Triple(null, false, null)
        }

        val isStringExpressionConcat = prefixPsi.leftOperand is StringLiteralExpression
        val isExpressionStringConcat = prefixPsi.rightOperand is StringLiteralExpression

        if (isStringExpressionConcat) {
            val prefix = (prefixPsi.leftOperand as StringLiteralExpression).contents
            return Triple(prefix, true, prefixPsi.rightOperand)
        } else if (isExpressionStringConcat) {
            val prefix = (prefixPsi.rightOperand as StringLiteralExpression).contents
            return Triple(prefix, true, prefixPsi.leftOperand)
        }

        return Triple(null, false, null)
    }

    private class OpenUrlLineMarkerInfo(
        private val prefix: String,
        private val size: String,
        private val needInput: Boolean,
        private val inputExpressionString: String?,
        element: PsiElement
    ) :
        RelatedItemLineMarkerInfo<PsiElement>(element, element.textRange, AllIcons.Ide.External_link_arrow,
            { "Open debug log page" }, null, GutterIconRenderer.Alignment.CENTER, { emptyList() }) {

        override fun createGutterRenderer(): GutterIconRenderer {
            return object : LineMarkerGutterIconRenderer<PsiElement?>(this) {
                override fun getClickAction(): AnAction {
                    return object : AnAction() {
                        override fun actionPerformed(e: AnActionEvent) {
                            val prefix = if (needInput) {
                                val input = MyUtils.invokeAndWaitResult {
                                    Messages.showInputDialog(
                                        e.project!!,
                                        "Enter value of '$inputExpressionString' expression\n to complete the log prefix:",
                                        "Open Debug Log",
                                        null,
                                        "0",
                                        null
                                    ) ?: "no value"
                                }
                                if (input == "no value") {
                                    return
                                }
                                prefix + input
                            } else {
                                prefix
                            }
                            val searchQuery = "buffer_prefix=$prefix&buffer_size=$size"
                            val url = Env.data.debugLogUrl + "?" + searchQuery
                            BrowserUtil.browse(url)
                        }
                    }
                }

                override fun isNavigateAction(): Boolean = true
            }
        }
    }
}
