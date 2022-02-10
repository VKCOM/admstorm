package com.vk.admstorm.git.sync.files

import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vcs.FileStatus
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.vk.admstorm.env.Env
import com.vk.admstorm.ui.MessageDialog
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MyPathUtils.foldUserHome
import com.vk.admstorm.utils.MyUiUtils
import com.vk.admstorm.utils.MyUtils
import git4idea.util.GitUIUtil.code
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.table.AbstractTableModel

class FilesNotSyncDialog(
    private val myProject: Project,
    files: List<RemoteFile>,
) : DialogWrapper(myProject, true) {

    private lateinit var myContentPanel: JPanel
    private lateinit var myFilesTablePanel: JPanel
    private lateinit var myFilesDifferLabel: JLabel
    private lateinit var myUseLocalVersionButton: JButton
    private lateinit var myUseRemoteVersionButton: JButton
    private lateinit var myShowDiffButton: JButton
    private val myFilesTable = JBTable(FilesTableModel())
    private val myUnresolvedFiles = mutableListOf<RemoteFile>()

    private val mySyncHandler = RemoteFileManager(myProject)

    init {
        myUnresolvedFiles.addAll(files.sortedBy { it.status() })

        title = "Files not Synchronized"

        myFilesDifferLabel.text = "The following files differ on ${Env.data.serverName} and locally:"

        myUseLocalVersionButton.text = "Use local"
        myUseLocalVersionButton.addActionListener {
            onResolve(true)
        }

        myUseRemoteVersionButton.text = "Use ${Env.data.serverName}"
        myUseRemoteVersionButton.addActionListener {
            onResolve(false)
        }

        myShowDiffButton.addActionListener {
            onSelect()
        }

        myFilesTable.selectionModel = DefaultListSelectionModel()
        myFilesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        myFilesTable.intercellSpacing = JBUI.emptySize()
        myFilesTable.setDefaultRenderer(Any::class.java, MyCellRenderer())

        myFilesTable.columnModel.getColumn(1).preferredWidth = 150
        myFilesTable.columnModel.getColumn(1).maxWidth = 150

        object : DoubleClickListener() {
            override fun onDoubleClick(e: MouseEvent): Boolean {
                onSelect()
                return true
            }
        }.installOn(myFilesTable)

        myFilesTable.selectionModel.addListSelectionListener { e ->
            onRowFocus(e)
        }

        val decoratedTable = ToolbarDecorator.createDecorator(myFilesTable).createPanel()

        myFilesTablePanel.add(decoratedTable, GridConstraints().apply {
            row = 0; column = 0; fill = GridConstraints.FILL_BOTH
        })

        if (myUnresolvedFiles.isNotEmpty()) {
            myFilesTable.setRowSelectionInterval(0, 0)
        }

        setOKButtonText("Resolve")
        setSize(650, 250)

        init()

        getButton(okAction)?.isEnabled = myUnresolvedFiles.isEmpty()
    }

    private fun onRowFocus(e: ListSelectionEvent) {
        if (e.valueIsAdjusting) return

        val rowIndex = myFilesTable.selectedRow
        if (rowIndex == -1) {
            myShowDiffButton.isEnabled = false
            return
        }

        myShowDiffButton.isEnabled = true

        val file = myUnresolvedFiles[rowIndex]
        myUseRemoteVersionButton.isEnabled = !file.isNotFound

        myUseRemoteVersionButton.text = when {
            file.isRenamed -> "Rename back on local"
            file.isRemoved -> "Remove on local"
            file.localFile.isRemoved -> "Get from ${Env.data.serverName}"
            else -> "Use ${Env.data.serverName}"
        }

        myUseLocalVersionButton.text = when {
            file.isRenamed -> "Rename on ${Env.data.serverName}"
            file.isNotFound -> "Send to ${Env.data.serverName}"
            file.isRemoved -> "Send to ${Env.data.serverName}"
            file.localFile.isRemoved -> "Remove on ${Env.data.serverName}"
            else -> "Use local"
        }
    }

    override fun getPreferredFocusedComponent() = myFilesTable

    private fun onResolve(useLocal: Boolean) {
        val selectedRow = myFilesTable.selectedRow
        val remoteFile = myUnresolvedFiles.getOrNull(selectedRow) ?: return

        val onResolve = {}

        when {
            remoteFile.localFile.isRemoved -> doResolveLocalFileRemoved(useLocal, remoteFile, onResolve)
            remoteFile.isRenamed -> doResolveLocalFileRenamed(useLocal, remoteFile, onResolve)
            else -> doResolveContentMismatch(remoteFile, useLocal, onResolve)
        }

        myUnresolvedFiles.removeAt(selectedRow)
        (myFilesTable.model as FilesTableModel).fireTableDataChanged()

        if (myUnresolvedFiles.isNotEmpty()) {
            myFilesTable.setRowSelectionInterval(0, 0)
        }

        getButton(okAction)?.isEnabled = myUnresolvedFiles.isEmpty()
    }

    private fun doResolveLocalFileRenamed(useLocal: Boolean, remoteFile: RemoteFile, onReady: Runnable) {
        if (useLocal) {
            mySyncHandler.renameRemoteFile(remoteFile, onReady)
        } else {
            mySyncHandler.renameLocalFile(remoteFile, onReady)
        }
    }

    private fun doResolveContentMismatch(
        remoteFile: RemoteFile,
        useLocal: Boolean,
        onReady: Runnable
    ) {
        val relativeFilePath = remoteFile.path
        val localFile = MyUtils.virtualFileByRelativePath(myProject, relativeFilePath)

        if (localFile == null) {
            MessageDialog.showWarning("Can't find local ${code(relativeFilePath)} file", "Overwrite Fail")
            return
        }

        if (useLocal) {
            mySyncHandler.rewriteRemoteFileWithLocalContent(localFile, onReady)
        } else {
            mySyncHandler.rewriteLocalFileWithRemoteContent(localFile, remoteFile.content, onReady)
        }
    }

    private fun doResolveLocalFileRemoved(useLocal: Boolean, remoteFile: RemoteFile, onReady: Runnable) {
        if (useLocal) {
            mySyncHandler.removeFileOnServer(remoteFile, onReady)
        } else {
            mySyncHandler.createLocalFileFromRemote(remoteFile, onReady)
        }
    }

    private fun onSelect() {
        val selectedRow = myFilesTable.selectedRow
        val remoteFile = myUnresolvedFiles.getOrNull(selectedRow) ?: return

        val localFile = MyUtils.virtualFileByRelativePath(myProject, remoteFile.path)
        val localFileText =
            if (localFile == null) null
            else LoadTextUtil.loadText(localFile).toString()

        val factory = NotSyncFilesViewerFactory(myProject, remoteFile, localFile?.path ?: "", localFileText)
        factory.viewer.showAndGet()
    }

    override fun createCenterPanel() = myContentPanel

    private inner class MyCellRenderer : ColoredTableCellRenderer() {

        private fun RemoteFile.statusTextAttributes() =
            SimpleTextAttributes(
                SimpleTextAttributes.STYLE_PLAIN, when {
                    isRenamed -> FileStatus.MODIFIED.color
                    isRemoved -> FileStatus.DELETED.color
                    isNotFound -> FileStatus.DELETED.color
                    localFile.isRemoved -> FileStatus.ADDED.color
                    else -> FileStatus.MODIFIED.color
                }
            )

        override fun customizeCellRenderer(
            table: JTable,
            value: Any?,
            selected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ) {
            val file = value as RemoteFile

            toolTipText = "Double click to view diff or file content"
            border = null

            when (column) {
                1 -> append(
                    file.status(),
                    file.statusTextAttributes()
                )
                0 -> {
                    icon = MyUiUtils.fileTypeIcon(file.path)

                    if (file.isRenamed && file.origPath != null) {
                        val absOrigPath = MyPathUtils.absoluteLocalPath(myProject, file.origPath)
                        val origFile = File(absOrigPath)

                        append(origFile.name, file.statusTextAttributes())
                        append(" ")
                        append(foldUserHome(origFile.parent), SimpleTextAttributes.GRAY_ATTRIBUTES)

                        append(" ${UIUtil.rightArrow()} ")

                        val absNewPath = MyPathUtils.absoluteLocalPath(myProject, file.path)
                        val newFile = File(absNewPath)

                        append(newFile.name)
                        append(" ")
                        append(foldUserHome(newFile.parent), SimpleTextAttributes.GRAY_ATTRIBUTES)

                        return
                    }

                    val absPath = MyPathUtils.absoluteLocalPath(myProject, file.path)
                    val iofile = File(absPath)

                    append(iofile.name, file.statusTextAttributes())
                    append(" ")
                    append(foldUserHome(iofile.parent), SimpleTextAttributes.GRAY_ATTRIBUTES)
                }
            }
        }
    }

    private inner class FilesTableModel : AbstractTableModel() {
        override fun getRowCount() = myUnresolvedFiles.size
        override fun getColumnCount() = 2

        override fun getColumnName(column: Int): String {
            return when (column) {
                0 -> "Name"
                1 -> "Status"
                else -> "unknown"
            }
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            return myUnresolvedFiles[rowIndex]
        }
    }
}
