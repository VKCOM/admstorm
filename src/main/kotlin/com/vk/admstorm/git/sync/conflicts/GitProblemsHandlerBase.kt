package com.vk.admstorm.git.sync.conflicts

import com.intellij.execution.Output
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.vk.admstorm.ui.MessageDialog
import com.vk.admstorm.utils.MyPathUtils

open class GitProblemsHandlerBase(protected val myProject: Project) {
    companion object {
        private val LOG = logger<GitProblemsHandlerBase>()
    }

    enum class State {
        Ok,
        Canceled,
        Handled,
    }

    protected fun showFilesDialog(
        output: Output,
        actionName: String,
        stashAndAction: Runnable,
        forceAction: Runnable
    ): State {
        LOG.info("Show dialog with a choice of actions to resolve the git conflict for $actionName")
        val files = conflictFiles(output)
        if (files.isEmpty()) {
            LOG.info("No files to show")
            return State.Ok
        }

        var wasCanceled = false

        ApplicationManager.getApplication().invokeAndWait {
            val choice = when (actionName) {
                "checkout" -> ConflictDialog.showForCheckout(myProject, files)
                "pull" -> ConflictDialog.showForPull(myProject, files)
                else -> return@invokeAndWait
            }
            LOG.info("Selected choice for $actionName is $choice")

            when (choice) {
                ConflictDialog.Choice.STASH -> stashAndAction.run()
                ConflictDialog.Choice.FORCE -> forceAction.run()
                ConflictDialog.Choice.CANCEL -> wasCanceled = true
            }
        }

        return if (wasCanceled) State.Canceled else State.Handled
    }

    private fun conflictFiles(output: Output): List<String> {
        return output.stderr
            .split("\n")
            .filter { it.startsWith("\t") }
            .map {
                MyPathUtils.absoluteLocalPath(myProject, it.removePrefix("\t"))
            }
    }

    protected fun showGenericWarningDialog(output: Output) {
        LOG.warn("showGenericWarningDialog method has been called")
        MessageDialog.showWarning(output.stderr, "Problem with git operation")
    }
}
