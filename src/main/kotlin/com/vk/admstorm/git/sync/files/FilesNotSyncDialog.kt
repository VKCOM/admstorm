package com.vk.admstorm.git.sync.files

import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vcs.FileStatus
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.vk.admstorm.ui.MessageDialog
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MyPathUtils.foldUserHome
import com.vk.admstorm.utils.MyUiUtils
import com.vk.admstorm.utils.MyUtils
import com.vk.admstorm.utils.ServerNameProvider
import git4idea.util.GitUIUtil.code
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.table.AbstractTableModel

class FilesNotSyncDialog(
    private val project: Project,
    files: List<RemoteFile>,
) : DialogWrapper(project, true) {
    private lateinit var useLocalVersionButton: Cell<JButton>
    private lateinit var useRemoteVersionButton: Cell<JButton>
    private lateinit var showDiffButton: Cell<JButton>

    private val filesTable = JBTable(FilesTableModel())
    private val unresolvedFiles = mutableListOf<RemoteFile>()

    private val fileManager = RemoteFileManager(project)

    init {
        unresolvedFiles.addAll(files.sortedBy { it.status() })
        title = "Files not Synchronized"

        setOKButtonText("Resolve")
        setSize(750, 250)
        init()

        getButton(okAction)?.isEnabled = unresolvedFiles.isEmpty()
    }

    override fun createCenterPanel(): JComponent {
        filesTable.selectionModel = DefaultListSelectionModel()
        filesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        filesTable.intercellSpacing = JBUI.emptySize()
        filesTable.setDefaultRenderer(Any::class.java, DiffCellRenderer())

        filesTable.columnModel.getColumn(1).preferredWidth = 150
        filesTable.columnModel.getColumn(1).maxWidth = 200

        object : DoubleClickListener() {
            override fun onDoubleClick(e: MouseEvent): Boolean {
                onSelect()
                return true
            }
        }.installOn(filesTable)

        if (unresolvedFiles.isNotEmpty()) {
            filesTable.setRowSelectionInterval(0, 0)
        }

        val filesPanel = panel {
            row {
                val decoratedTable = ToolbarDecorator.createDecorator(filesTable).createPanel()

                cell(decoratedTable)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .verticalAlign(VerticalAlign.FILL)

            }.resizableRow()
        }

        val buttonsPanel = panel {
            buttonsGroup {
                row {
                    useLocalVersionButton = button("Use local") { onResolve(true) }
                        .horizontalAlign(HorizontalAlign.FILL)
                }

                row {
                    useRemoteVersionButton = button("Use ${ServerNameProvider.name()}") { onResolve(false) }
                        .horizontalAlign(HorizontalAlign.FILL)
                }

                row {
                    showDiffButton = button("Show diff") { onSelect() }
                        .horizontalAlign(HorizontalAlign.FILL)
                }
            }
        }

        val panel = panel {
            row {
                label("The following files differ on ${ServerNameProvider.name()} and locally:")
            }

            row {
                cell(filesPanel)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .verticalAlign(VerticalAlign.FILL)
                    .resizableColumn()

                cell(buttonsPanel)
                    .horizontalAlign(HorizontalAlign.RIGHT)
                    .verticalAlign(VerticalAlign.FILL)

            }.resizableRow()
        }

        filesTable.selectionModel.addListSelectionListener { e ->
            onRowFocus(e)
        }

        return panel
    }

    private fun onRowFocus(e: ListSelectionEvent) {
        if (e.valueIsAdjusting) return

        val rowIndex = filesTable.selectedRow
        if (rowIndex == -1) {
            showDiffButton.component.isEnabled = false
            return
        }

        showDiffButton.component.isEnabled = true

        val file = unresolvedFiles[rowIndex]

        useRemoteVersionButton.component.text = when {
            file.isRenamed           -> "Rename back on local"
            file.isRemoved           -> "Remove on local"
            file.isNotFound          -> "Remove on local"
            file.localFile.isRemoved -> "Get from ${ServerNameProvider.name()}"
            else                     -> "Use ${ServerNameProvider.name()}"
        }

        useLocalVersionButton.component.text = when {
            file.isRenamed           -> "Rename on ${ServerNameProvider.name()}"
            file.isNotFound          -> "Send to ${ServerNameProvider.name()}"
            file.isRemoved           -> "Send to ${ServerNameProvider.name()}"
            file.localFile.isRemoved -> "Remove on ${ServerNameProvider.name()}"
            else                     -> "Use local"
        }
    }

    override fun getPreferredFocusedComponent() = filesTable

    private fun onResolve(useLocal: Boolean) {
        val selectedRow = filesTable.selectedRow
        val remoteFile = unresolvedFiles.getOrNull(selectedRow) ?: return

        val onResolve = {}

        when {
            (remoteFile.isNotFound || remoteFile.isRemoved) && !useLocal -> doRemoveLocalFile(remoteFile, onResolve)
            remoteFile.localFile.isRemoved                               -> doResolveLocalFileRemoved(useLocal, remoteFile, onResolve)
            remoteFile.isRenamed                                         -> doResolveLocalFileRenamed(useLocal, remoteFile, onResolve)
            else                                                         -> doResolveContentMismatch(remoteFile, useLocal, onResolve)
        }

        unresolvedFiles.removeAt(selectedRow)
        (filesTable.model as FilesTableModel).fireTableDataChanged()

        if (unresolvedFiles.isNotEmpty()) {
            filesTable.requestFocus()
            filesTable.setRowSelectionInterval(0, 0)
        }

        getButton(okAction)?.isEnabled = unresolvedFiles.isEmpty()
    }

    private fun doResolveLocalFileRenamed(useLocal: Boolean, remoteFile: RemoteFile, onReady: Runnable) {
        if (useLocal) {
            fileManager.revertRemoteFileToOriginal(remoteFile, onReady)
        } else {
            fileManager.renameLocalFile(remoteFile, onReady)
        }
    }

    private fun doResolveContentMismatch(
        remoteFile: RemoteFile,
        useLocal: Boolean,
        onReady: Runnable,
    ) {
        val relativeFilePath = remoteFile.path
        val localFile = MyUtils.virtualFileByRelativePath(project, relativeFilePath)

        if (localFile == null) {
            MessageDialog.showWarning("Can't find local ${code(relativeFilePath)} file", "Overwrite Fail")
            return
        }

        if (useLocal) {
            fileManager.rewriteRemoteFileWithLocalContent(localFile, onReady)
        } else {
            fileManager.rewriteLocalFileWithRemoteContent(localFile, remoteFile.content, onReady)
        }
    }

    private fun doResolveLocalFileRemoved(useLocal: Boolean, remoteFile: RemoteFile, onReady: Runnable) {
        if (useLocal) {
            fileManager.removeRemoteFile(remoteFile, onReady)
        } else {
            fileManager.createLocalFileFromRemote(remoteFile, onReady)
        }
    }

    private fun doRemoveLocalFile(remoteFile: RemoteFile, onReady: Runnable) {
        fileManager.removeLocalFile(remoteFile, onReady)
    }

    private fun onSelect() {
        val selectedRow = filesTable.selectedRow
        val remoteFile = unresolvedFiles.getOrNull(selectedRow) ?: return

        val localFile = MyUtils.virtualFileByRelativePath(project, remoteFile.path)
        val localFileText =
            if (localFile == null) null
            else LoadTextUtil.loadText(localFile).toString()

        val factory = NotSyncFilesViewerFactory(project, remoteFile, localFile?.path ?: "", localFileText)
        factory.viewer.showAndGet()
    }

    private inner class DiffCellRenderer : ColoredTableCellRenderer() {

        private fun RemoteFile.statusTextAttributes() =
            SimpleTextAttributes(
                SimpleTextAttributes.STYLE_PLAIN, when {
                    isRenamed           -> FileStatus.MODIFIED.color
                    isRemoved           -> FileStatus.DELETED.color
                    isNotFound          -> FileStatus.DELETED.color
                    localFile.isRemoved -> FileStatus.ADDED.color
                    else                -> FileStatus.MODIFIED.color
                }
            )

        override fun customizeCellRenderer(
            table: JTable,
            value: Any?,
            selected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int,
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
                        val absOrigPath = MyPathUtils.absoluteLocalPath(project, file.origPath)
                        val origFile = File(absOrigPath)

                        append(origFile.name, file.statusTextAttributes())
                        append(" ")
                        append(foldUserHome(origFile.parent), SimpleTextAttributes.GRAY_ATTRIBUTES)

                        append(" ${UIUtil.rightArrow()} ")

                        val absNewPath = MyPathUtils.absoluteLocalPath(project, file.path)
                        val newFile = File(absNewPath)

                        append(newFile.name)
                        append(" ")
                        append(foldUserHome(newFile.parent), SimpleTextAttributes.GRAY_ATTRIBUTES)

                        return
                    }

                    val absPath = MyPathUtils.absoluteLocalPath(project, file.path)
                    val ioFile = File(absPath)

                    append(ioFile.name, file.statusTextAttributes())
                    append(" ")
                    append(foldUserHome(ioFile.parent), SimpleTextAttributes.GRAY_ATTRIBUTES)
                }
            }
        }
    }

    private inner class FilesTableModel : AbstractTableModel() {
        override fun getRowCount() = unresolvedFiles.size
        override fun getColumnCount() = 2

        override fun getColumnName(column: Int): String {
            return when (column) {
                0    -> "Name"
                1    -> "Status"
                else -> "unknown"
            }
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            return unresolvedFiles[rowIndex]
        }
    }
}
