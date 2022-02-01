package com.vk.admstorm.actions

import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.PhpPsiUtil

/**
 * Base class for actions that run on a specific PHP [PsiElement].
 *
 * To use, override the [actionPerformed] method and the [errorHint] parameter.
 */
abstract class PhpPsiElementAction<T : PsiElement>(aClass: Class<T>) :
    SelectionBasedPsiElementAction<T>(aClass, PhpFile::class.java) {

    override fun getElementFromSelection(file: PsiFile, selectionModel: SelectionModel): T? {
        val selectionStart = selectionModel.selectionStart
        val selectionEnd = selectionModel.selectionEnd
        return PhpPsiUtil.findElementOfClassAtRange(file, selectionStart, selectionEnd, myClass, true, true)
    }
}
