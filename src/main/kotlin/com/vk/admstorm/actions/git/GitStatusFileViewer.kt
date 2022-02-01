package com.vk.admstorm.actions.git

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.util.DiffUserDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.fileTypes.ex.FileTypeChooser
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vcs.FileStatus
import com.intellij.util.ui.JBUI
import com.vk.admstorm.env.Env
import com.vk.admstorm.utils.MyUtils
import git4idea.index.GitFileStatus
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.border.Border

class GitStatusFileViewer(
    project: Project,
    file: GitFileStatus,
    fileContent: String,
) : DialogWrapper(project, true) {
    companion object {
        private val LOG = Logger.getInstance(GitStatusFileViewer::class.java)
    }

    private var myMainComponent: JComponent
    private var mySingleEditor: EditorEx? = null

    init {
        val isModified = file.getStagedStatus() == FileStatus.MODIFIED ||
                file.getUnStagedStatus() == FileStatus.MODIFIED

        myMainComponent = if (isModified) {
            val filepath = file.path.path
            val localFile = MyUtils.virtualFileByName(filepath)
            val localFileContent =
                if (localFile == null) null
                else LoadTextUtil.loadText(localFile).toString()

            if (localFileContent == null) {
                createSingleEditorForFile(project, file, fileContent)
            } else {
                createDiffForFiles(project, file, fileContent, localFileContent)
            }
        } else {
            createSingleEditorForFile(project, file, fileContent)
        }

        title = "${Env.data.serverName.replaceFirstChar { it.uppercase() }} File Content"

        init()
    }

    private fun createSingleEditorForFile(
        project: Project,
        file: GitFileStatus,
        fileContent: String
    ): JComponent {
        val document = EditorFactory.getInstance().createDocument(fileContent)
        mySingleEditor = EditorFactory.getInstance().createEditor(document, project) as EditorEx

        val fileType = FileTypeChooser.getKnownFileTypeOrAssociate(file.path.name)
        if (fileType != null) {
            val highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType)
            try {
                mySingleEditor!!.highlighter = highlighter
            } catch (e: Throwable) {
                LOG.warn("Unexpected exception while createSingleEditorForFile", e)
            }
        }
        document.setReadOnly(true)
        return mySingleEditor!!.component
    }

    private fun createDiffForFiles(
        project: Project,
        remoteFile: GitFileStatus,
        remoteFileContent: String,
        localFileContent: String
    ): JComponent {
        val type = FileTypeChooser.getKnownFileTypeOrAssociate(remoteFile.path.name)
        val content1 = DiffContentFactory.getInstance().create(remoteFileContent, type)
        val content2 = DiffContentFactory.getInstance().create(localFileContent, type)
        content1.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true)
        content2.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true)
        val request = SimpleDiffRequest(
            "${Env.data.serverName} vs Local",
            content1,
            content2,
            "Current ${Env.data.serverName} version",
            "Current local version"
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
