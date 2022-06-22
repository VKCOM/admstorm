package com.vk.admstorm.git.sync.branches

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.ssh.LostConnectionHandler
import com.vk.admstorm.utils.ServerNameProvider
import git4idea.branch.GitBrancher
import git4idea.repo.GitRepositoryManager

class LocalBranchSwitcher(private val myProject: Project) {
    companion object {
        private val LOG = logger<LocalBranchSwitcher>()
    }

    fun switch(branchName: String, onReady: Runnable? = null) {
        doSwitchTask(branchName, onReady)
    }

    private fun doSwitchTask(branchName: String, onReady: Runnable? = null) {
        ProgressManager.getInstance().run(object : Task.Modal(
            myProject,
            "Checkout to $branchName locally",
            true,
        ) {
            override fun run(indicator: ProgressIndicator) {
                switchAction(branchName, indicator) {
                    onReady?.run()
                    LOG.info("Switched to a branch '$branchName' locally")
                }
            }
        })
    }

    private fun switchAction(
        branchName: String,
        indicator: ProgressIndicator,
        onReady: Runnable? = null
    ) {
        if (!GitUtils.localBranchExist(myProject, branchName)) {
            // If the branch does not exist, then we must first fetch
            // the server branch to local, in order to then switch to it.
            LOG.info("Branch '$branchName' not found locally, start git pull branch from server")
            switchActionIfBranchNotExist(branchName, indicator, onReady)
            return
        }

        localSwitch(branchName, indicator, onReady)
    }

    private fun localSwitch(branchName: String, indicator: ProgressIndicator, onReady: Runnable? = null) {
        indicator.text2 = "Checkout to $branchName"
        val repos = GitRepositoryManager.getInstance(myProject).repositories
        GitBrancher.getInstance(myProject)
            .checkout(branchName, false, repos) {
                indicator.text2 = "Complete"
                indicator.fraction = 1.0
                onReady?.run()
            }
    }

    private fun switchActionIfBranchNotExist(
        branchName: String,
        indicator: ProgressIndicator,
        onReady: Runnable? = null
    ) {
        indicator.text2 = "Fetch $branchName from ${ServerNameProvider.name()}"
        val cmd = "git pull ${ServerNameProvider.name()} $branchName"
        val output = CommandRunner.runLocally(myProject, cmd)

        indicator.fraction = 0.5

        if (output.exitCode == 0) {
            localSwitch(branchName, indicator, onReady)
            return
        }

        if (LostConnectionHandler.handle(myProject, output) {
                switchActionIfBranchNotExist(branchName, indicator, onReady)
            }) return

        LOG.warn("git pull unexpected problem:\n${output.stdout}\n${output.stderr}")
    }
}
