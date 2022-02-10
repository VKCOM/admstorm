package com.vk.admstorm.actions.git

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
import com.vk.admstorm.env.Env
import com.vk.admstorm.git.sync.files.GitStatus.isDeleted
import com.vk.admstorm.utils.MyPathUtils.foldUserHome
import com.vk.admstorm.utils.MyUiUtils
import git4idea.index.GitFileStatus
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.AbstractTableModel

class GitStatusFilesDialog(
    private val myProject: Project,
    private val myGitStatusFiles: List<GitFileStatus>,
    private val myFilesContents: List<String>,
) : DialogWrapper(myProject, true) {

    private lateinit var myContentPanel: JPanel
    private lateinit var myFilesTablePanel: JPanel
    private lateinit var myTextLabel: JLabel
    private val myFilesTable = JBTable(FilesTableModel())
    private var myDiscardChangesSelected = false

    init {
        title = "Git Status Files on ${Env.data.serverName}"

        myTextLabel.text =
            "The following files on ${Env.data.serverName} are preventing the push from ${Env.data.serverName} to Gitlab:"

        myFilesTable.selectionModel = DefaultListSelectionModel()
        myFilesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        myFilesTable.intercellSpacing = JBUI.emptySize()
        myFilesTable.setDefaultRenderer(Any::class.java, MyCellRenderer())

        myFilesTable.columnModel.getColumn(1).preferredWidth = 100
        myFilesTable.columnModel.getColumn(1).maxWidth = 100

        object : DoubleClickListener() {
            override fun onDoubleClick(e: MouseEvent): Boolean {
                onSelect()
                return true
            }
        }.installOn(myFilesTable)

        val decoratedTable = ToolbarDecorator.createDecorator(myFilesTable).createPanel()

        myFilesTablePanel.add(decoratedTable, GridConstraints().apply {
            row = 0; column = 0; fill = GridConstraints.FILL_BOTH
        })

        if (myGitStatusFiles.isNotEmpty()) {
            myFilesTable.setRowSelectionInterval(0, 0)
        }

        okAction.putValue(Action.SHORT_DESCRIPTION, "Stash all changes and Push")
        cancelAction.putValue(Action.SHORT_DESCRIPTION, "Don't Push")

        okAction.putValue(FOCUSED_ACTION, true)

        setOKButtonText("Stash")
        setSize(650, 250)

        init()
    }

    fun isDiscardChangesSelected() = myDiscardChangesSelected

    override fun createActions(): Array<Action> {
        val discardChangesAction = object : DialogWrapperAction("Discard Changes") {
            override fun doAction(e: ActionEvent?) {
                doOKAction()
                myDiscardChangesSelected = true
            }
        }

        discardChangesAction.putValue(Action.SHORT_DESCRIPTION, "Discards all changes and Push")

        return arrayOf(cancelAction, discardChangesAction, okAction)
    }

    private fun onSelect() {
        val selectedRow = myFilesTable.selectedRow
        val file = myGitStatusFiles.getOrNull(selectedRow) ?: return
        val remoteFileContent = myFilesContents.getOrNull(selectedRow) ?: return

        GitStatusFilesViewerFactory(myProject, file, file.path.path, remoteFileContent).viewer.showAndGet()
    }

    override fun getPreferredFocusedComponent() = myFilesTable
    override fun createCenterPanel() = myContentPanel

    private inner class MyCellRenderer : ColoredTableCellRenderer() {
        override fun customizeCellRenderer(
            table: JTable,
            value: Any?,
            selected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ) {
            val file = value as GitFileStatus

            toolTipText = "Double click to view file content"
            border = null

            when (column) {
                1 -> append(file.statusText())
                0 -> {
                    icon = MyUiUtils.fileTypeIcon(file.path.name)

                    append(
                        value.path.name,
                        file.statusTextAttributes()
                    )

                    val parentPath = value.path.parentPath
                    if (parentPath != null) {
                        append(" ")
                        append(foldUserHome(parentPath.path), SimpleTextAttributes.GRAY_ATTRIBUTES)
                    }
                }
            }
        }

        private fun GitFileStatus.statusText(): String {
            val status = getStagedStatus()
            if (status != null) {
                return status.text
            }

            val unStagedStatus = getUnStagedStatus()
            if (unStagedStatus != null) {
                return unStagedStatus.text
            }

            return getFileStatusString(this)
        }

        private fun getFileStatusString(file: GitFileStatus) = when {
            file.isUntracked() -> "Untracked"
            file.isIgnored() -> "Ignored"
            file.isConflicted() -> "Conflicted"
            file.isDeleted() -> "Deleted"
            else -> ""
        }

        private fun GitFileStatus.statusTextAttributes(): SimpleTextAttributes {
            val status = getStagedStatus()
            if (status != null) {
                return SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, status.color)
            }

            val unStagedStatus = getUnStagedStatus()
            if (unStagedStatus != null) {
                return SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, unStagedStatus.color)
            }

            return getFileStatusTextAttributes(this)
        }

        private fun getFileStatusTextAttributes(file: GitFileStatus) = SimpleTextAttributes(
            SimpleTextAttributes.STYLE_PLAIN, when {
                file.isUntracked() -> FileStatus.IGNORED.color
                file.isIgnored() -> FileStatus.IGNORED.color
                file.isConflicted() -> FileStatus.MERGED_WITH_CONFLICTS.color
                file.isDeleted() -> FileStatus.DELETED.color
                else -> FileStatus.IGNORED.color
            }
        )
    }

    private inner class FilesTableModel : AbstractTableModel() {
        override fun getRowCount() = myGitStatusFiles.size
        override fun getColumnCount() = 2

        override fun getColumnName(column: Int): String {
            return when (column) {
                0 -> "Name"
                1 -> "Status"
                else -> "unknown"
            }
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            return when (columnIndex) {
                0, 1 -> myGitStatusFiles[rowIndex]
                else -> {}
            }
        }
    }
}
