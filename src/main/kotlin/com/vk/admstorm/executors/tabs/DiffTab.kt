package com.vk.admstorm.executors.tabs

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffContext
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.tools.simple.SimpleDiffViewer
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.vk.admstorm.executors.SimpleComponentWithActions

class DiffTab(private val myProject: Project, name: String) : BaseTab(name) {
    private val myDiffViewer: SimpleDiffViewer

    init {
        myDiffViewer = SimpleDiffViewer(
            object : DiffContext() {
                override fun isFocusedInWindow() = false
                override fun requestFocusInWindow() {}
                override fun getProject() = myProject
                override fun isWindowFocused() = false
            },
            SimpleDiffRequest(
                "Diff",
                DiffContentFactory.getInstance().create(myProject, ""),
                DiffContentFactory.getInstance().create(myProject, ""),
                "KPHP output",
                "PHP output"
            )
        )

        myDiffViewer.init()
    }

    fun withKphpOutput(output: String) {
        val document = myDiffViewer.content1.document
        withDocumentOutput(output, document)
    }

    fun withPhpOutput(output: String) {
        val document = myDiffViewer.content2.document
        withDocumentOutput(output, document)
    }

    override fun componentWithActions() =
        SimpleComponentWithActions(myDiffViewer.component, myDiffViewer.component)

    override fun componentToFocus() = myDiffViewer.component

    override fun afterAdd() = myDiffViewer.rediff()

    private fun withDocumentOutput(output: String, document: Document) {
        WriteCommandAction.runWriteCommandAction(myProject) {
            document.setReadOnly(false)
            document.deleteString(0, document.text.length)
            document.insertString(0, output)
            document.setReadOnly(true)
            myDiffViewer.rediff()
        }
    }
}
