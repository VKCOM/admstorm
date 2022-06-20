package com.vk.admstorm.psi.references

import com.intellij.javascript.web.codeInsight.WebSymbolReferenceProvider.Companion.startOffsetIn
import com.intellij.lang.javascript.JSDocTokenTypes
import com.intellij.lang.javascript.JSTokenTypes
import com.intellij.lang.javascript.psi.jsdoc.JSDocComment
import com.intellij.openapi.paths.WebReference
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.css.impl.CssElementTypes
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocRef
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.vk.admstorm.AdmService
import com.vk.admstorm.env.Env

class JiraReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // /** @see DT-100 */ for PHP
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement().withParent(PhpDocTag::class.java),
            JiraCommonReferenceProvider()
        )
        // line comments in PHP, JS, CSS
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PsiComment::class.java).andNot(
                PlatformPatterns.psiElement(PsiDocCommentBase::class.java)
            ),
            JiraCommonReferenceProvider()
        )
        // /** See DT-100 */ for PHP
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement().withParent(PhpDocComment::class.java),
            JiraCommonReferenceProvider()
        )
        // /** See DT-100 */ or /** @see DT-100 */ for JS
        // For some reason,
        //   PlatformPatterns.psiElement().withParent(JSDocComment::class.java)
        // doesn't work for JS, so we need this workaround.
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(JSDocComment::class.java),
            JiraMultilineJsCommentReferenceProvider()
        )
    }
}

class JiraMultilineJsCommentReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(docComment: PsiElement, context: ProcessingContext): Array<PsiReference> {
        // In registerReferenceProviders we can't get a project, so we need to check it here.
        if (!AdmService.getInstance(docComment.project).needBeEnabled()) return PsiReference.EMPTY_ARRAY
        if (docComment !is JSDocComment) return PsiReference.EMPTY_ARRAY

        val identifiers = PsiTreeUtil.collectElements(docComment) {
            it.elementType == JSDocTokenTypes.DOC_COMMENT_DATA
        }

        val references = mutableListOf<PsiReference>()

        identifiers.forEach {
            references.addAll(
                JiraCommonReferenceProvider.getReferences(
                    docComment,
                    it.text,
                    fastCheck = true,
                    it.startOffsetIn(docComment)
                )
            )
        }

        return references.toTypedArray()
    }
}

class JiraCommonReferenceProvider : PsiReferenceProvider() {
    companion object {
        private val JIRA_TASK_REGEX = "[A-Z][A-Z_\\d]+-\\d+".toRegex()

        /**
         * @param fastCheck whenever [text] contains exactly one word
         */
        fun getReferences(
            element: PsiElement,
            text: String,
            fastCheck: Boolean = false,
            rightShift: Int = 0,
        ): Array<PsiReference> {
            if (text.isEmpty()) {
                return PsiReference.EMPTY_ARRAY
            }
            if (fastCheck && (!text.last().isDigit() || !text.first().isLetter())) {
                return PsiReference.EMPTY_ARRAY
            }
            if (!text.contains('-')) {
                return PsiReference.EMPTY_ARRAY
            }

            val tasks = JIRA_TASK_REGEX.findAll(text).toList()
            return tasks.map {
                val range = TextRange(it.range.first, it.range.last + 1).shiftRight(rightShift)
                WebReference(element, range, "https://jira.${Env.data.serviceDomain}.com/browse/${it.value}")
            }.toTypedArray()
        }
    }

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        // In registerReferenceProviders we can't get a project, so we need to check it here.
        if (!AdmService.getInstance(element.project).needBeEnabled()) return PsiReference.EMPTY_ARRAY

        val value = if (
            element is PhpDocRef ||
            element.elementType == PhpTokenTypes.LINE_COMMENT ||
            element.elementType == PhpTokenTypes.DOC_IDENTIFIER ||
            element.elementType == JSTokenTypes.END_OF_LINE_COMMENT ||
            element.elementType == CssElementTypes.CSS_COMMENT
        ) {
            element.text
        } else {
            return PsiReference.EMPTY_ARRAY
        }

        val fastCheck = element is PhpDocRef || element.elementType == PhpTokenTypes.DOC_IDENTIFIER
        return getReferences(element, value, fastCheck)
    }
}
