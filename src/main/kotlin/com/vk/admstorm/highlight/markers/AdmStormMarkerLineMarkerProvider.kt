package com.vk.admstorm.highlight.markers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.PhpDocCommentImpl
import com.jetbrains.php.lang.documentation.phpdoc.psi.impl.tags.PhpDocTagImpl
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.impl.ClassConstImpl
import com.jetbrains.php.lang.psi.elements.impl.ParameterListImpl
import com.vk.admstorm.env.Env
import com.vk.admstorm.highlight.markers.admmarker.*
import com.vk.admstorm.utils.extensions.unquote
import com.vk.admstorm.utils.php.PhpFieldUtils
import com.vk.admstorm.utils.php.PhpFunctionUtils

class AdmStormMarkerLineMarkerProvider : LineMarkerProvider {
    companion object {
        private val tagName = "@admstorm-marker"

        private fun calcService(serviceName: String, project: Project): IMarker? {
            return when (serviceName) {
                "lang"           -> LangMarker(project)
                "confdata"       -> ConfdataMarker(project)
                "part"           -> PartMarker(project)
                "config"         -> ConfigMarker(project)
                "buggerLog"      -> BufferMarker()
                "statsHouseView" -> StatshouseMarker(StatshouseMarker.Mode.VIEW)
                "logger"         -> LoggerMarker()

                else             -> null
            }
        }
    }

    override fun getLineMarkerInfo(element: PsiElement) = null

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: LineMarkerInfos) {
        if (!Env.isResolved()) {
            return
        }

        for (element in elements) {
            if (element.elementType != PhpTokenTypes.IDENTIFIER) {
                continue
            }

            val call = element.parent as? FunctionReference ?: continue

            val func = call.resolve() as? Function ?: continue
            if (func.docComment !is PhpDocCommentImpl) {
                continue
            }

            val parameterList = call.parameterList
            if (parameterList !is ParameterListImpl) {
                continue
            }

            val markerElements = (func.docComment as PhpDocCommentImpl).getTagElementsByName(tagName)
            if (markerElements.size != 1) {
                continue
            }

            val marker = markerElements.first()
            if (marker !is PhpDocTagImpl) {
                continue
            }

            val markerItem = AdmStormMarkerItem(marker)
            if (!markerItem.isValueNode() || !markerItem.isValidValue()) {
                continue
            }

            val service = calcService(markerItem.kindValue(), element.project) ?: continue

            var containingClass: PhpClass? = null
            var callClass: PhpClass? = null
            if (call is MethodReference) {
                containingClass = (func as Method).containingClass

                val classReference = call.classReference
                if (classReference is PhpReference) {
                    val resolvePhpClass = classReference.resolve()
                    if (resolvePhpClass is PhpClass) {
                        callClass = resolvePhpClass
                    }
                }
            }

            val qValues = markerItem.qValue().split("{", "}")
            val arg = PhpFunctionUtils.parametersPsi(func, parameterList)

            val keyName = parseQValue(qValues, arg, callClass, containingClass) ?: continue

            val text = service.getTooltip()
            val lineMarkerInfo = LineMarkerInfo(
                element,
                element.textRange,
                service.getIcon(),
                { text },
                ServiceGutterIconNavigationHandler(keyName, service),
                GutterIconRenderer.Alignment.CENTER,
                { text },
            )

            result.add(lineMarkerInfo)
        }
    }

    private fun parseQValue(
        qValues: List<String>,
        arg: Map<String, PsiElement?>,
        callClass: PhpClass?,
        containingClass: PhpClass?,
    ): String? {
        var keyName = ""
        for (qValue in qValues) {
            if (qValue.startsWith("$")) {
                val argName = qValue.removePrefix("$")
                val argItem = arg[argName] ?: return null

                val value = parseVariable(argItem) ?: return null
                keyName += value.unquote()
            } else {
                val typeList = listOf("self", "parent", "static")
                val type = typeList.firstOrNull { qValue.startsWith(it) }

                if (type != null) {
                    if (callClass == null) {
                        return null
                    }

                    val constName = qValue.removePrefix("$type::")
                    val value = parseConst(containingClass, callClass, type, constName) ?: return null
                    keyName += value

                } else {
                    keyName += qValue
                }
            }
        }

        return keyName
    }

    private fun parseVariable(psiElement: PsiElement): String? {
        if (psiElement is ConstantReference) {
            val constantPsi = psiElement.resolve() as? Constant ?: return null
            val stringPsi = constantPsi.value as? StringLiteralExpression ?: return null
            if (stringPsi.children.isNotEmpty()) {
                return null
            }

            return stringPsi.contents
        }

        if (psiElement is ClassConstantReference) {
            val constantPsi = psiElement.resolve() as? ClassConstImpl ?: return null
            val stringPsi = constantPsi.children.first() as? StringLiteralExpression ?: return null
            if (stringPsi.children.isNotEmpty()) {
                return null
            }

            return stringPsi.contents
        }

        if (psiElement is StringLiteralExpression) {
            if (psiElement.children.isNotEmpty()) {
                return null
            }

            return psiElement.contents
        }

        if (psiElement is FieldReference) {
            if (!psiElement.isStatic) {
                return null
            }

            val constantPsi = psiElement.resolve() as? Field ?: return null
            val stringPsi = constantPsi.children.first() as? StringLiteralExpression ?: return null
            if (stringPsi.children.isNotEmpty()) {
                return null
            }

            return stringPsi.contents
        }

        if (psiElement is PhpExpression) {
            if (psiElement.children.isNotEmpty()) {
                return null
            }

            if (psiElement.reference != null) {
                return null
            }

            return psiElement.firstChild.text
        }

        return null
    }

    private fun parseConst(containingClass: PhpClass?, callClass: PhpClass, type: String, fieldName: String): String? {
        val filed = PhpFieldUtils.getFiledByTypeInClass(containingClass, callClass, type, fieldName, true) ?: return null
        if (filed !is ClassConstImpl) {
            return null
        }

        return filed.defaultValuePresentation?.unquote()?.unquote()
    }

    private class AdmStormMarkerItem(node: PhpDocTagImpl) {
        private val nodeItem = node.text.split(" ", limit = 2)
        private val value = nodeItem[1].split(" ", limit = 2)

        fun kindValue() = value[0].removePrefix("kind=")

        fun qValue() = value[1].removePrefix("q=")

        fun isValueNode() = nodeItem.size == 2
        fun isValidValue() = value.size == 2
    }

}
