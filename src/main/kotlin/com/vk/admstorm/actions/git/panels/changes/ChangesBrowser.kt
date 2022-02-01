package com.vk.admstorm.actions.git.panels.changes

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.openapi.vcs.changes.ui.ChangeNodeDecorator
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserBase
import com.intellij.openapi.vcs.changes.ui.SimpleChangesBrowser
import com.intellij.openapi.vcs.changes.ui.TreeModelBuilder
import javax.swing.tree.DefaultTreeModel

/**
 * Simple viewer for some changes. Such a viewer is used, for example, in the push
 * window to change a specific commit. See [SimpleChangesBrowser] for details.
 *
 * This class can be used directly as a component since it inherits from JPanel.
 *
 * As soon as you need to display any changes in it, use the [setChangesToDisplay]
 * method to which pass the necessary changes for display.
 *
 * Note that your changes must inherit from the [Change] class.
 *
 * To construct it, you usually need two objects of the [ContentRevision] class.
 * See [GitLocalContentRevision] for an example.
 *
 * Note that if you pass `beforeRevision` or `afterRevision` params as null to
 * [Change] constructor, the diff viewer will display the file as added or deleted,
 * respectively.
 *
 * To get the file to be colored in the desired color depending on the state, look
 * at the [Change] constructor with three arguments.
 *
 * Note that both revisions must be non-null in it.
 *
 * Node that the colors will be automatically set if you pass one null to the
 * constructor with two parameters.
 *
 * Note that after passing the changes, the viewer will immediately build a tree
 * of changes, taking into account the file paths from the changes, and will also
 * configure the diff viewer, no additional actions are required.
 */
class ChangesBrowser(project: Project) : ChangesBrowserBase(project, false, false) {
    private val myChanges = mutableListOf<Change>()
    private var myChangeNodeDecorator: ChangeNodeDecorator? = null

    init {
        init()
    }

    override fun buildTreeModel(): DefaultTreeModel {
        return TreeModelBuilder.buildFromChanges(myProject, grouping, myChanges, myChangeNodeDecorator)
    }

    /**
     * Sets the changes to be displayed in the viewer and rebuilds the tree.
     */
    fun setChangesToDisplay(changes: Collection<Change>) {
        myChanges.clear()
        myChanges.addAll(changes)
        myViewer.rebuildTree()
    }
}
