package com.vk.admstorm.utils.php

import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.PhpClass

object PhpFieldUtils {
    fun getParentFieldByName(phpClass: PhpClass, filedName: String, findConstant: Boolean): Field? {
        val parentClass = phpClass.superClass ?: return null

        return parentClass.findFieldByName(filedName, findConstant)
    }

    fun getFiledByTypeInClass(
        containingClass: PhpClass?,
        callClass: PhpClass,
        type: String,
        fieldName: String,
        findConstant: Boolean,
    ): Field? {
        return when (type) {
            "self" -> containingClass?.findFieldByName(fieldName, findConstant)
            "static" -> callClass.findFieldByName(fieldName, findConstant)
            "parent" -> getParentFieldByName(callClass, fieldName, findConstant)

            else -> null
        }
    }
}
