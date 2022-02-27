package com.vk.admstorm.git.sync

import com.intellij.execution.Output
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.vk.admstorm.settings.AdmStormSettingsState
import com.vk.admstorm.settings.GitConflictResolutionStrategy
import com.vk.admstorm.ui.MessageDialog
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.ServerNameProvider
import git4idea.util.GitUIUtil.bold

class GitErrorHandler(private val myProject: Project) {
    companion object {
        private val LOG = Logger.getInstance(GitErrorHandler::class.java)

        private const val NoTrackingInformationWarningMessage = "no tracking information"
        private const val NotFoundRemoteRef = "Couldn't find remote ref"
    }

    enum class State {
        Ok,
        Canceled,
        Handled
    }

    private fun handlePullBranchWithoutTrackingInformation(branchName: String) {
        val message = """
            It seems that the ${bold(branchName)} branch does not have tracking information. 
            
            Push the branch to the Gitlab first to be able to git pull it.
        """.trimIndent()

        MessageDialog.showWarning(message, "No Tracking Information")
    }

    fun handlePull(output: Output, branchName: String) {
        if (output.stderr.contains(NoTrackingInformationWarningMessage)) {
            handlePullBranchWithoutTrackingInformation(branchName)
            return
        }

        if (output.stderr.contains(NotFoundRemoteRef)) {
            handlePullBranchWithoutTrackingInformation(branchName)
            return
        }

        MessageDialog.showWarning(output.stderr, "Git Pull Failed")
    }

    fun handleCheckout(
        output: Output,
        stashAndCheckoutCommand: Runnable,
        forceCheckoutCommand: Runnable
    ): State {
        if (output.exitCode == 0) {
            return State.Ok
        }

        if (!output.stderr.contains("checkout")) {
            showGenericWarningDialog(output)
            return State.Handled
        }

        when (AdmStormSettingsState.getInstance().gitConflictResolutionStrategy) {
            GitConflictResolutionStrategy.Stash -> stashAndCheckoutCommand.run()
            GitConflictResolutionStrategy.ForceCheckout -> forceCheckoutCommand.run()
            else -> return ask(output, stashAndCheckoutCommand, forceCheckoutCommand)
        }

        return State.Handled
    }

    private fun conflictFiles(output: Output): List<String> {
        return output.stderr
            .split("\n")
            .filter { it.startsWith("\t") }
            .map {
                MyPathUtils.absoluteLocalPath(myProject, it.removePrefix("\t"))
            }
    }

    private fun ask(
        output: Output,
        stashAndCheckoutCommand: Runnable,
        forceCheckoutCommand: Runnable
    ): State {
        val files = conflictFiles(output)

        var wasCanceled = false

        ApplicationManager.getApplication().invokeAndWait {
            val choice = CheckoutConflictDialog.show(myProject, files)
            when (choice) {
                CheckoutConflictDialog.Choice.STASH -> stashAndCheckoutCommand.run()
                CheckoutConflictDialog.Choice.FORCE -> forceCheckoutCommand.run()
                CheckoutConflictDialog.Choice.CANCEL -> wasCanceled = true
            }
        }

        return if (wasCanceled) State.Canceled else State.Handled
    }

    private fun showGenericWarningDialog(output: Output) {
        LOG.warn("showGenericWarningDialog method has been called")
        MessageDialog.showWarning(output.stderr, "${ServerNameProvider.name()} Git Warning")
    }
}
