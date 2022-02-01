package com.vk.admstorm.configuration.problems.panels

import com.intellij.dvcs.push.ui.TooltipNode
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.impl.CompoundIconProvider
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.PsiUtilCore
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.vk.admstorm.actions.git.panels.TreeBaseNode
import javax.swing.Icon

class FileNameTreeNode(
    private val myProject: Project,
    val file: VirtualFile,
    val countProblems: Int,
) : TreeBaseNode(), NavigatableTreeNode, TooltipNode {

    private var myIcon: Icon? = null
    private var myLocation: String? = null

    private fun preload() {
        if (myIcon != null) {
            return
        }

        myIcon =
            CompoundIconProvider.findIcon(PsiUtilCore.findFileSystemItem(myProject, file), 0)
                ?: when (file.isDirectory) {
                    true -> AllIcons.Nodes.Folder
                    else -> AllIcons.FileTypes.Any_type
                }

        val url = file.parent?.presentableUrl
        if (url != null) {
            myLocation = FileUtil.getLocationRelativeToUserHome(url)
        }
    }

    override fun render(renderer: ColoredTreeCellRenderer) {
        preload()

        renderer.icon = myIcon
        renderer.append(file.name)

        if (myLocation != null) {
            renderer.append("  $myLocation", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }

        renderer.append("  $countProblems problems", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }

    override fun getNavigatable() = OpenFileDescriptor(myProject, file, 0, 0)

    override fun getTooltip() = "Double click to go to sources"
}
