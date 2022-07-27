package com.vk.admstorm.tags.phptags

import com.jetbrains.php.lang.documentation.phpdoc.parser.tags.PhpDocParamTagParser
import com.jetbrains.php.lang.documentation.phpdoc.parser.tags.PhpDocTagParser
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocElementType
import com.jetbrains.php.lang.parser.PhpParserErrors
import com.jetbrains.php.lang.parser.PhpPsiBuilder

class AdmStormMarkerTag : PhpDocTagParser() {
    companion object {
        private val paramsParamTagParser = PhpDocParamTagParser()
        private val admStromMarker = PhpDocElementType("AdmStormMarker")
    }

    private fun PhpPsiBuilder.expected(s: String): Boolean {
        this.error(PhpParserErrors.expected(s))
        return false
    }

    private fun parseVariableContent(builder: PhpPsiBuilder): Boolean {
        if (!builder.compareAndEat(DOC_LBRACE)) {
            return builder.expected("{")
        }

        if (!builder.compare(DOC_IDENTIFIER) && !builder.compare(DOC_TEXT) && !builder.compare(DOC_VARIABLE)) {
            return builder.expected("self/static or \$variable")
        }

        if (builder.compare(DOC_IDENTIFIER)) {
            if (builder.tokenText != "self" && builder.tokenText != "static" && builder.tokenText != "parent") {
                return builder.expected("self/static/parent")
            } else {
                builder.advanceLexer()
            }

            if (!builder.compareAndEat(DOC_STATIC)) {
                return builder.expected("::")
            }

            if (!builder.compareAndEat(DOC_IDENTIFIER)) {
                return builder.expected("static field name")
            }
        } else if (!paramsParamTagParser.parseContents(builder)) {
            return builder.expected("\$variable")
        }

        if (!builder.compareAndEat(DOC_RBRACE)) {
            return builder.expected("}")
        }

        return true
    }

    override fun parseContents(builder: PhpPsiBuilder): Boolean {
        val marker = builder.mark()

        if (builder.compare(DOC_IDENTIFIER) && builder.tokenText == "kind") {
            builder.advanceLexer()
        } else {
            marker.drop()
            return builder.expected("kind")
        }

        if (builder.compare(DOC_TEXT) && builder.tokenText == "=") {
            builder.advanceLexer()
        } else {
            marker.drop()
            return builder.expected("=")
        }

        var needKindIdentifier = false
        while (builder.compare(DOC_IDENTIFIER) || builder.compare(DOC_TEXT)) {
            if (builder.tokenText == "q") {
                break
            }

            builder.advanceLexer()
            needKindIdentifier = true
        }

        if (!needKindIdentifier) {
            marker.drop()
            return builder.expected("kind name")
        }

        if (builder.compare(DOC_IDENTIFIER) && builder.tokenText == "q") {
            builder.advanceLexer()
        } else {
            marker.drop()
            return builder.expected("q")
        }

        if (builder.compare(DOC_TEXT) && builder.tokenText!!.startsWith("=")) {
            builder.advanceLexer()
        } else {
            marker.drop()
            return builder.expected("=")
        }

        var needNextIdentifier = false
        while (builder.compare(DOC_TEXT) ||
            builder.compare(DOC_IDENTIFIER) ||
            builder.compare(DOC_LBRACE) ||
            builder.compare(DOC_AMPERSAND)
        ) {
            if (builder.compare(DOC_LBRACE)) {
                if (!parseVariableContent(builder)) {
                    marker.drop()
                    return false
                } else {
                    needNextIdentifier = true
                }
            } else {
                builder.advanceLexer()
                needNextIdentifier = true
            }
        }

        if (!needNextIdentifier) {
            marker.drop()
            return builder.expected("text or {\$variable}")
        }

        marker.done(admStromMarker)
        return true
    }
}
