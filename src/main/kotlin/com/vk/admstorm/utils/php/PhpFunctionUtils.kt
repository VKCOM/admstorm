package com.vk.admstorm.utils.php

import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.impl.ParameterListImpl

object PhpFunctionUtils {
    fun parameters(function: Function, parameterList: ParameterListImpl): Map<String, String?> {
        val paramsMap = mutableMapOf<String, String?>()
        for (param in function.parameters) {
            paramsMap[param.name] = param.defaultValuePresentation
        }

        val nameIdentifiers = parameterList.namedArgumentsParts.map { it.nameIdentifier.text }
        for (nameIdentifier in nameIdentifiers) {
            paramsMap[nameIdentifier] = parameterList.getParameter(nameIdentifier)?.text
        }

        val noNamedArgumentsSize = parameterList.parameters.size - nameIdentifiers.size
        for (i in 0 until noNamedArgumentsSize) {
            val paramName = function.parameters.getOrNull(i)?.name ?: break
            paramsMap[paramName] = parameterList.getParameter(i)?.text
        }

        return paramsMap
    }

    fun parametersPsi(function: Function, parameterList: ParameterListImpl): Map<String, PsiElement?> {
        val paramsMap = mutableMapOf<String, PsiElement?>()

        val nameIdentifiers = parameterList.namedArgumentsParts.map { it.nameIdentifier.text }
        for (nameIdentifier in nameIdentifiers) {
            paramsMap[nameIdentifier] = parameterList.getParameter(nameIdentifier)
        }

        val noNamedArgumentsSize = parameterList.parameters.size - nameIdentifiers.size
        for (i in 0 until noNamedArgumentsSize) {
            val paramName = function.parameters.getOrNull(i)?.name ?: break
            paramsMap[paramName] = parameterList.getParameter(i)
        }

        return paramsMap
    }
}
