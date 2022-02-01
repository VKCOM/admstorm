package com.vk.admstorm.git.sync.files

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.DiffRequestPanel
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.util.DiffUserDataKeys
import com.intellij.openapi.fileTypes.ex.FileTypeChooser
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import com.vk.admstorm.env.Env
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.border.Border

class FilesDiffViewer(
    project: Project,
    remoteFile: RemoteFile,
    localFileContent: String,
) : DialogWrapper(project, true) {

    private var myDiffRequestPanel: DiffRequestPanel

    init {
        val remoteContent = if (remoteFile.isNotFound) "<file not found>" else remoteFile.content
        val type = FileTypeChooser.getKnownFileTypeOrAssociate(remoteFile.path)
        val content1 = DiffContentFactory.getInstance().create(remoteContent, type)
        val content2 = DiffContentFactory.getInstance().create(localFileContent, type)
        content2.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true)
        val request =
            SimpleDiffRequest("${Env.data.serverName} vs Local", content1, content2, Env.data.serverName, "Local")

        myDiffRequestPanel = DiffManager.getInstance().createRequestPanel(project, {}, null)
        myDiffRequestPanel.setRequest(request)

        title = "${Env.data.serverName.replaceFirstChar { it.uppercase() }} vs Local"

        init()
    }

    override fun createContentPaneBorder(): Border? = null

    override fun createSouthPanel(): JComponent? {
        return super.createSouthPanel().apply {
            border = JBUI.Borders.empty(8, 12)
        }
    }

    override fun createCenterPanel() = JBUI.Panels.simplePanel()
        .addToCenter(myDiffRequestPanel.component).apply {
            preferredSize = Dimension(890, 500)
        }
}
