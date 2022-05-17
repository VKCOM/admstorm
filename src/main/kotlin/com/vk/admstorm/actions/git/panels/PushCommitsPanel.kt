package com.vk.admstorm.actions.git.panels

import com.intellij.dvcs.push.PushSettings
import com.intellij.dvcs.push.ui.EditableTreeNode
import com.intellij.dvcs.push.ui.TooltipNode
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.committed.CommittedChangesTreeBrowser
import com.intellij.ui.*
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.ui.DefaultTreeUI
import com.intellij.util.containers.Convertor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.vcs.log.ui.details.commit.CommitDetailsPanel
import com.vk.admstorm.actions.git.panels.changes.ChangesBrowser
import com.vk.admstorm.git.sync.commits.Commit
import one.util.streamex.StreamEx
import java.awt.BorderLayout
import java.awt.event.MouseEvent
import java.util.*
import java.util.function.Consumer
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class PushCommitsPanel(
    private val myProject: Project,
    commits: List<Commit>,
    count: Int,
    private val myCommitNodeBuilder: (Commit) -> CommitTreeNode,
    rootNodeBuilder: () -> TreeBaseNode,
) : JPanel() {

    private val myScrollPane: JScrollPane
    private var myTree: CheckboxTree = CheckboxTree()
    private val myTreeCellRenderer: MyTreeCellRenderer
    private val myChangesBrowser: ChangesBrowser
    private val myShowDetailsAction: MyShowDetailsAction
    private val myDetailsPanel: CommitDetailsPanel

    init {
        val splitter = OnePixelSplitter(0.5f)

        myDetailsPanel = CommitDetailsPanel()
        val detailsScrollPane = JBScrollPane(
            myDetailsPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        )
        detailsScrollPane.border = JBUI.Borders.empty()
        detailsScrollPane.viewportBorder = JBUI.Borders.empty()
        val detailsContentPanel = BorderLayoutPanel()
        detailsContentPanel.addToCenter(detailsScrollPane)

        if (commits.isNotEmpty()) {
            myDetailsPanel.setCommit(commits[0].presentation(myProject))
        }

        myChangesBrowser = ChangesBrowser(myProject)
        myChangesBrowser.hideViewerBorder()
        myChangesBrowser.viewer.setEmptyText(DvcsBundle.message("push.no.commits.selected"))

        val detailsSplitter = OnePixelSplitter(true, 0.67f)
        detailsSplitter.firstComponent = myChangesBrowser
        detailsSplitter.secondComponent = detailsContentPanel

        myShowDetailsAction = MyShowDetailsAction(myProject) { state: Boolean ->
            detailsSplitter.secondComponent = if (state) detailsContentPanel else null
        }
        myShowDetailsAction.isEnabled = false
        myChangesBrowser.addToolbarSeparator()
        myChangesBrowser.addToolbarAction(myShowDetailsAction)

        val rootNode = rootNodeBuilder()
        buildTree(commits, count, rootNode)

        myTreeCellRenderer = MyTreeCellRenderer()
        myTree = object : CheckboxTree(myTreeCellRenderer, rootNode) {
            override fun shouldShowBusyIconIfNeeded(): Boolean {
                return true
            }

            override fun isPathEditable(path: TreePath): Boolean {
                return isEditable && path.lastPathComponent is DefaultMutableTreeNode
            }

            override fun onNodeStateChanged(node: CheckedTreeNode) {
                if (node is EditableTreeNode) {
                    (node as EditableTreeNode).fireOnSelectionChange(node.isChecked)
                }
            }

            override fun getToolTipText(event: MouseEvent): String {
                val path: TreePath = myTree.getPathForLocation(event.x, event.y) ?: return ""
                val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return ""
                if (node is TooltipNode) {
                    if (node is ShowMoreCommitsNode) {
                        return (node as TooltipNode).tooltip
                    }

                    val select = DvcsBundle.message("push.select.all.commit.details")
                    return (node as TooltipNode).tooltip + "<p style='font-style:italic;color:gray;'>" + select + "</p>"
                }
                return ""
            }

            override fun installSpeedSearch() {
                TreeSpeedSearch(this, Convertor { path: TreePath ->
                    val pathComponent = path.lastPathComponent
                    if (pathComponent is ServerRepoTreeNode) {
                        return@Convertor pathComponent.getCurrentBranchName()
                    }
                    if (pathComponent is LocalRepoTreeNode) {
                        return@Convertor pathComponent.getCurrentBranchName()
                    }
                    if (pathComponent is CommitTreeNode) {
                        return@Convertor pathComponent.commit.subject
                    }
                    pathComponent.toString()
                })
            }
        }
        myTree.setUI(DefaultTreeUI())
        myTree.border = JBUI.Borders.emptyTop(10)
        myTree.isEditable = false
        myTree.showsRootHandles = rootNode.childCount > 1
        myTree.invokesStopCellEditing = true
        myTree.isRootVisible = true
        myTree.selectionModel?.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
        myTree.rowHeight = 0
        myTree.showsRootHandles = false
        myTree.addTreeSelectionListener { updateChangesView() }

        myTree.selectionPath = TreeUtil.getFirstNodePath(myTree)

        myScrollPane = JBScrollPane(myTree)
        myScrollPane.getViewport().scrollMode = JViewport.SIMPLE_SCROLL_MODE
        myScrollPane.setOpaque(false)
        myScrollPane.setBorder(JBUI.Borders.empty())

        splitter.firstComponent = myScrollPane
        splitter.secondComponent = detailsSplitter

        layout = BorderLayout()
        border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
        add(splitter)
    }

    private fun buildTree(
        commits: List<Commit>,
        count: Int,
        rootNode: TreeBaseNode
    ) {
        if (count > commits.size) {
            val commitsExceptLast = commits.dropLast(1)
            commitsExceptLast.forEach {
                rootNode.add(myCommitNodeBuilder(it))
            }
            rootNode.add(ShowMoreCommitsNode(count - commits.size))
            rootNode.add(myCommitNodeBuilder(commits.last()))
            return
        }

        commits.forEach {
            rootNode.add(myCommitNodeBuilder(it))
        }
    }

    fun getPreferredFocusedComponent(): JComponent = myTree

    private fun getSelectedCommitNodes(): List<CommitTreeNode> {
        val selectedNodes = getSelectedTreeNodes()
        return if (selectedNodes.isEmpty()) emptyList() else
            collectSelectedCommitNodes(selectedNodes)
    }

    private fun collectSelectedCommitNodes(selectedNodes: List<DefaultMutableTreeNode>): List<CommitTreeNode> {
        val nodes = StreamEx.of(selectedNodes)
            .select(ServerRepoTreeNode::class.java)
            .toFlatList { node ->
                getChildNodesByType(
                    node,
                    CommitTreeNode::class.java, true
                )
            }

        nodes.addAll(
            StreamEx.of(selectedNodes)
                .select(CommitTreeNode::class.java)
                .filter { node ->
                    !nodes.contains(
                        node
                    )
                }
                .toList())

        return nodes
    }

    private inline fun <reified T> getChildNodesByType(
        node: DefaultMutableTreeNode,
        type: Class<T>,
        reverseOrder: Boolean
    ): List<T> {
        val nodes: MutableList<T> = ArrayList()
        if (node.childCount < 1) {
            return nodes
        }
        var childNode = node.firstChild as DefaultMutableTreeNode?
        while (childNode != null) {
            if (type.isInstance(childNode)) {
                val nodeT = childNode as T
                if (reverseOrder) {
                    nodes.add(0, nodeT)
                } else {
                    nodes.add(nodeT)
                }
            }
            childNode = node.getChildAfter(childNode) as DefaultMutableTreeNode?
        }
        return nodes
    }

    private fun getSelectedTreeNodes(): List<DefaultMutableTreeNode> {
        val rows = myTree.selectionRows
        return if (rows != null && rows.isNotEmpty())
            getNodesForRows(getSortedRows(rows))
        else emptyList()
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

    private fun collectAllChanges(commitNodes: List<CommitTreeNode>): List<Change> {
        return CommittedChangesTreeBrowser.zipChanges(collectChanges(commitNodes))
    }

    private fun collectChanges(commitNodes: List<CommitTreeNode>): List<Change> {
        val changes = mutableListOf<Change>()
        commitNodes.forEach { node ->
            changes.addAll(node.commit.getChanges(myProject))
        }
        return changes
    }

    private fun updateChangesView() {
        val commitNodes = getSelectedCommitNodes()
        if (commitNodes.isNotEmpty()) {
            myChangesBrowser.viewer.setEmptyText(DvcsBundle.message("push.no.differences"))
        } else {
            myChangesBrowser.viewer.setEmptyText(DvcsBundle.message("push.no.commits.selected"))
        }

        myChangesBrowser.setChangesToDisplay(collectAllChanges(commitNodes))

        if (commitNodes.size == 1 &&
            getSelectedTreeNodes()
                .stream()
                .noneMatch { it is ServerRepoTreeNode }
        ) {
            myDetailsPanel.setCommit(commitNodes[0].getPresentation(myProject))
            myShowDetailsAction.isEnabled = true
        } else {
            myShowDetailsAction.isEnabled = false
        }
    }

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

    private class MyShowDetailsAction(project: Project, onUpdate: Consumer<Boolean>) :
        ToggleActionButton(DvcsBundle.message("push.show.details"), AllIcons.Actions.PreviewDetailsVertically),
        DumbAware {
        private val mySettings: PushSettings
        private val myOnUpdate: Consumer<Boolean>

        init {
            mySettings = project.getService(PushSettings::class.java)
            myOnUpdate = onUpdate
        }

        private val value: Boolean
            get() = mySettings.showDetailsInPushDialog

        override fun isSelected(e: AnActionEvent): Boolean {
            return value
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            mySettings.showDetailsInPushDialog = state
            myOnUpdate.accept(state)
        }

        override fun setEnabled(enabled: Boolean) {
            myOnUpdate.accept(enabled && value)
            super.setEnabled(enabled)
        }
    }
}
