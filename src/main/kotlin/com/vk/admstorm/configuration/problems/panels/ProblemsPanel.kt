package com.vk.admstorm.configuration.problems.panels

import com.intellij.dvcs.push.ui.EditableTreeNode
import com.intellij.ide.DefaultTreeExpander
import com.intellij.ide.TreeExpander
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.*
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.ui.DefaultTreeUI
import com.intellij.util.OpenSourceUtil
import com.intellij.util.containers.Convertor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import com.vk.admstorm.actions.git.panels.TreeBaseNode
import java.awt.BorderLayout
import java.util.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class ProblemsPanel(private val myProject: Project, problems: List<Problem>) : JPanel(), DataProvider {
    private val myScrollPane: JScrollPane
    private var myTree: CheckboxTree = CheckboxTree()
    private val myTreeCellRenderer: MyTreeCellRenderer
    private val myTreeExpander: TreeExpander

    private val myRootNode = object : TreeBaseNode() {
        override fun render(renderer: ColoredTreeCellRenderer) {}
    }

    init {
        buildTree(problems)

        myTreeCellRenderer = MyTreeCellRenderer()
        myTree = object : CheckboxTree(myTreeCellRenderer, myRootNode) {
            override fun installSpeedSearch() {
                TreeSpeedSearch(this, Convertor { path: TreePath ->
                    val pathComponent = path.lastPathComponent
                    if (pathComponent is FileNameTreeNode) {
                        return@Convertor pathComponent.file.path
                    }
                    if (pathComponent is ProblemTreeNode) {
                        return@Convertor "${pathComponent.problem.name} ${pathComponent.problem.description}"
                    }
                    pathComponent.toString()
                })
            }

            override fun onDoubleClick(node: CheckedTreeNode?) {
                if (node is NavigatableTreeNode) {
                    OpenSourceUtil.navigate(true, node.getNavigatable())
                }
            }
        }

        myTree.setUI(DefaultTreeUI())
        myTree.isRootVisible = false
        myTree.selectionModel?.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        myTree.showsRootHandles = true
        myTree.addTreeSelectionListener { updateChangesView() }

        myTree.selectionPath = TreeUtil.getFirstNodePath(myTree)

        myTree.emptyText.text = "No problems found"

        myTreeExpander = DefaultTreeExpander(myTree)
        myTreeExpander.expandAll()

        PopupHandler.installPopupMenu(
            myTree,
            "AdmProblemsView.ToolWindow.TreePopup",
            "AdmProblemsView.ToolWindow.TreePopup"
        )

        myScrollPane = JBScrollPane(myTree)
        myScrollPane.getViewport().scrollMode = JViewport.SIMPLE_SCROLL_MODE
        myScrollPane.setOpaque(false)
        myScrollPane.setBorder(JBUI.Borders.empty())

        layout = BorderLayout()
        border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)

        add(myScrollPane)
    }

    override fun getData(dataId: String): Any? {
        val selected = getSelectedTreeNode()

        if (selected != null && selected is NavigatableTreeNode) {
            if (CommonDataKeys.NAVIGATABLE.`is`(dataId)) return selected.getNavigatable()
            if (CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId)) {
                val navigatable = selected.getNavigatable()
                return navigatable?.let { arrayOf(it) }
            }
        }

        return null
    }

    private fun buildTree(problems: List<Problem>) {
        val filesProblems = mutableMapOf<VirtualFile, MutableList<Problem>>()
        problems.forEach {
            if (it.file == null) {
                return@forEach
            }

            val fileProblems: MutableList<Problem> = filesProblems[it.file] ?: mutableListOf()
            fileProblems.add(it)
            filesProblems[it.file] = fileProblems
        }

        filesProblems.forEach { (file, problems) ->
            val rootNode = FileNameTreeNode(myProject, file, problems.size)

            problems.forEach {
                rootNode.add(ProblemTreeNode(myProject, it))
            }

            myRootNode.add(rootNode)
        }
    }

    fun getPreferredFocusedComponent(): JComponent = myTree

    private fun getSelectedTreeNode(): DefaultMutableTreeNode? {
        val rows = myTree.selectionRows
        return if (rows != null && rows.isNotEmpty())
            getNodesForRows(getSortedRows(rows)).firstOrNull()
        else null
    }

    private fun getSortedRows(rows: IntArray): List<Int> {
        val sorted = mutableListOf<Int>()
        rows.forEach {
            sorted.add(it)
        }
        sorted.sortWith(Collections.reverseOrder())
        return sorted
    }

    private fun getNodesForRows(rows: List<Int>): List<DefaultMutableTreeNode> {
        val nodes = mutableListOf<DefaultMutableTreeNode>()
        rows.forEach {
            val path = myTree.getPathForRow(it)
            val pathComponent = path?.lastPathComponent
            if (pathComponent is DefaultMutableTreeNode) {
                nodes.add(pathComponent)
            }
        }
        return nodes
    }

    private fun updateChangesView() {}

    private class MyTreeCellRenderer : CheckboxTree.CheckboxTreeCellRenderer() {
        override fun customizeRenderer(
            tree: JTree,
            value: Any,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            if (value !is DefaultMutableTreeNode) {
                return
            }
            myCheckbox.border = null
            myCheckbox.isVisible = false

            val renderer = textRenderer
            renderer.ipad = JBUI.insets(0, 10)

            val userObject = value.userObject

            if (value is EditableTreeNode) {
                value.render(renderer)
            } else {
                renderer.append(userObject?.toString() ?: "")
            }
        }
    }
}
