package com.vk.admstorm.git.sync.conflicts

import com.intellij.execution.Output
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.vk.admstorm.settings.AdmStormSettingsState
import com.vk.admstorm.settings.GitConflictResolutionStrategy

class GitCheckoutProblemsHandler(project: Project) : GitProblemsHandlerBase(project) {
    companion object {
        private val LOG = logger<GitCheckoutProblemsHandler>()
    }

    fun handle(output: Output, stashAndAction: Runnable, forceAction: Runnable): State {
        if (output.exitCode == 0) return State.Ok

        val stderr = output.stderr
        if (stderr.isNotEmpty()) {
            LOG.warn("Git error appear: $stderr")
        }

        if (!stderr.contains("checkout")) {
            showGenericWarningDialog(output)
            return State.Handled
        }

        when (AdmStormSettingsState.getInstance().gitConflictResolutionStrategy) {
            GitConflictResolutionStrategy.Stash -> stashAndAction.run()
            GitConflictResolutionStrategy.ForceCheckout -> forceAction.run()
            else -> return showFilesDialog(output, "checkout", stashAndAction, forceAction)
        }

        return State.Handled
    }
}
