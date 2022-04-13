package com.vk.admstorm.git.sync.conflicts

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import com.vk.admstorm.utils.MyUiUtils
import com.vk.admstorm.utils.ServerNameProvider
import git4idea.DialogManager
import git4idea.util.GitSimplePathsBrowser
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JComponent

class ConflictDialog(
    private val myProject: Project,
    private val myFiles: List<String>,
    private val myActionName: String,
) : DialogWrapper(myProject, true, IdeModalityType.PROJECT) {
    private val myLowerCaseActionName = myActionName.lowercase()

    companion object {
        private val LOG = Logger.getInstance(ConflictDialog::class.java)

        private const val FORCE_EXIT_CODE = NEXT_USER_EXIT_CODE

        fun showForCheckout(project: Project, files: List<String>): Choice {
            val dialog = ConflictDialog(project, files, "Checkout")
            DialogManager.show(dialog)
            return Choice.fromDialogExitCode(dialog.exitCode)
        }

        fun showForPull(project: Project, files: List<String>): Choice {
            val dialog = ConflictDialog(project, files, "Pull")
            DialogManager.show(dialog)
            return Choice.fromDialogExitCode(dialog.exitCode)
        }
    }

    enum class Choice {
        STASH, FORCE, CANCEL;

        companion object {
            fun fromDialogExitCode(exitCode: Int) = when (exitCode) {
                OK_EXIT_CODE -> STASH
                FORCE_EXIT_CODE -> FORCE
                CANCEL_EXIT_CODE -> CANCEL
                else -> {
                    LOG.error("Unexpected exit code: $exitCode")
                    CANCEL
                }
            }
        }
    }

    init {
        title = "Git $myActionName Problem on ${ServerNameProvider.name()}"

        setOKButtonText("Stash and $myActionName")
        setCancelButtonText("Don't $myActionName")

        okAction.putValue(Action.SHORT_DESCRIPTION, "Creates a new stash and $myActionName")
        cancelAction.putValue(Action.SHORT_DESCRIPTION, "Don't $myActionName")

        okAction.putValue(FOCUSED_ACTION, true)

        init()
    }

    override fun createCenterPanel(): JComponent {
        return JBUI.Panels.simplePanel(0, 2)
            .addToTop(
                MyUiUtils.createTextInfoComponent(
                    """
                        Your local changes to the following files would be overwritten by $myLowerCaseActionName:
                    """.trimIndent()
                )
            )
            .addToCenter(GitSimplePathsBrowser(myProject, myFiles))
    }

    override fun createLeftSideActions(): Array<Action> {
        val forceCheckoutAction = object : DialogWrapperAction("Force $myActionName") {
            override fun doAction(e: ActionEvent?) {
                close(FORCE_EXIT_CODE)
            }
        }

        forceCheckoutAction.putValue(Action.SHORT_DESCRIPTION, "Discards all changes and $myActionName")

        return arrayOf(forceCheckoutAction)
    }
}
