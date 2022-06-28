package com.vk.admstorm.utils.extensions

fun String.normalizeSlashes() = replace("\\", "/")

/**
 * Returns the indent for the string.
 */
fun String.indentWidth(): Int = indexOfFirst { !it.isWhitespace() }.let { if (it == -1) length else it }

/**
 * Corrects indentation for the given string.
 *
 * What does broken indentation mean?
 * This is when the first line is indented less than all the others,
 * while all the rest are indented the same.
 *
 * In such a case, the function returns a string with the correct
 * indentation for the first line, so that all indentations are the
 * same size.
 *
 * This can be useful if code has been selected in the editor,
 * starting at some line character and continuing to the end of some block:
 *
 *    $a = <start-selection>array_map(function($el) {
 *        return $el * 2;
 *    }, [1, 2, 3]);<end-selection>
 *
 *    Selected text:
 *
 *    |array_map(function($el) {
 *    |       return $el * 2;
 *    |   }, [1, 2, 3]);
 *
 *    After executing function:
 *
 *    |   array_map(function($el) {
 *    |       return $el * 2;
 *    |   }, [1, 2, 3]);
 */
fun String.fixIndent(): String {
    val lines = lines()

    val firstLine = lines[0]
    val firstLineIndent = firstLine.indentWidth()

    val minCommonIndent = lines
        .slice(1 until lines.size)
        .filter { it.isNotBlank() }
        .minOfOrNull { it.indentWidth() } ?: 0

    if (firstLineIndent < minCommonIndent) {
        val diff = minCommonIndent - firstLineIndent
        return " ".repeat(diff) + this
    }

    return this
}

fun String.unquote() = removeSurrounding("\"").removeSurrounding("'")
