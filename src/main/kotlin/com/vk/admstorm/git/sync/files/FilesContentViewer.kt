package com.vk.admstorm.git.sync.files

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.util.DiffUserDataKeys
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import com.vk.admstorm.utils.MyPathUtils.foldUserHome
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.border.Border

class FilesContentViewer(
    project: Project,
    type: FileType?,
    firstContent: Content?,
    secondContent: Content?,
) : DialogWrapper(project, true) {

    companion object {
        private val LOG = logger<FilesContentViewer>()
    }

    data class Content(
        val title: String,
        val description: String,
        val path: String,
        val data: String,
    )

    private var myMainComponent: JComponent
    private var mySingleEditor: EditorEx? = null

    init {
        if (firstContent == null && secondContent == null) {
            throw IllegalArgumentException("At least one content must be non-null")
        }

        val needSingleEditor = firstContent == null || secondContent == null
        val content = firstContent?.data ?: secondContent!!.data

        myMainComponent = if (needSingleEditor) {
            createSingleViewer(project, type, content)
        } else {
            createDiffViewer(
                project, type,
                firstContent!!,
                secondContent!!,
            )
        }

        val path = firstContent?.path ?: secondContent!!.path

        title = if (needSingleEditor) {
            "${foldUserHome(path)} content from ${firstContent?.title ?: secondContent!!.title}"
        } else {
            "${firstContent!!.title} vs ${secondContent!!.title}"
        }

        init()
    }

    private fun createSingleViewer(
        project: Project,
        type: FileType?,
        content: String
    ): JComponent {
        val document = EditorFactory.getInstance().createDocument(content)
        mySingleEditor = EditorFactory.getInstance().createEditor(document, project) as EditorEx

        if (type != null) {
            val highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, type)
            try {
                mySingleEditor!!.highlighter = highlighter
            } catch (e: Throwable) {
                LOG.warn("Unexpected exception while createSingleEditorForFile", e)
            }
        }

        document.setReadOnly(true)
        return mySingleEditor!!.component
    }

    private fun createDiffViewer(
        project: Project,
        type: FileType?,
        firstContent: Content,
        secondContent: Content,
    ): JComponent {
        val content1 = DiffContentFactory.getInstance().create(firstContent.data, type)
        val content2 = DiffContentFactory.getInstance().create(secondContent.data, type)
        content1.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true)
        content2.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true)
        val request = SimpleDiffRequest(
            "${firstContent.title} vs ${secondContent.title}",
            content1,
            content2,
            firstContent.description,
            secondContent.description
        )

        val requestPanel = DiffManager.getInstance().createRequestPanel(project, {}, null)
        requestPanel.setRequest(request)

        return requestPanel.component
    }

    override fun createContentPaneBorder(): Border? = null

    override fun createSouthPanel(): JComponent =
        super.createSouthPanel().apply {
            border = JBUI.Borders.empty(8, 12)
        }

    override fun createCenterPanel() = JBUI.Panels.simplePanel()
        .addToCenter(myMainComponent).apply {
            preferredSize = Dimension(890, 500)
        }

    override fun dispose() {
        if (mySingleEditor != null && !mySingleEditor!!.isDisposed) {
            EditorFactory.getInstance().releaseEditor(mySingleEditor!!)
        }

        super.dispose()
    }
}
