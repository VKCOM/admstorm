package com.vk.admstorm.playground

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffContext
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.tools.simple.SimpleDiffViewer
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project

class KphpPhpDiffViewer(project: Project) : SimpleDiffViewer(
    object : DiffContext() {
        override fun isFocusedInWindow() = false
        override fun requestFocusInWindow() {}
        override fun getProject() = project
        override fun isWindowFocused() = false
    },
    SimpleDiffRequest(
        "Diff",
        DiffContentFactory.getInstance().create(project, ""),
        DiffContentFactory.getInstance().create(project, ""),
        "KPHP output",
        "PHP output"
    )
) {
    init {
        init()
    }

    fun clear() {
        withKphpOutput("")
        withPhpOutput("")
    }

    fun withKphpOutput(output: String) {
        withDocumentOutput(output, content1.document)
    }

    fun withPhpOutput(output: String) {
        withDocumentOutput(output, content2.document)
    }

    private fun withDocumentOutput(output: String, document: Document) {
        WriteCommandAction.runWriteCommandAction(myProject) {
            document.setReadOnly(false)
            document.deleteString(0, document.text.length)
            document.insertString(0, output)
            document.setReadOnly(true)
            rediff()
        }
    }
}
