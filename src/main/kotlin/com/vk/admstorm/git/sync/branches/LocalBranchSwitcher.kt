package com.vk.admstorm.git.sync.branches

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.env.Env
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.ssh.LostConnectionHandler
import git4idea.branch.GitBrancher
import git4idea.repo.GitRepositoryManager

class LocalBranchSwitcher(private val myProject: Project) {
    companion object {
        private val LOG = Logger.getInstance(LocalBranchSwitcher::class.java)
    }

    fun switch(branchName: String, force: Boolean, onReady: Runnable? = null) {
        doSwitchTask(branchName, force, onReady)
    }

    private fun doSwitchTask(branchName: String, force: Boolean, onReady: Runnable? = null) {
        ProgressManager.getInstance().run(object : Task.Modal(
            myProject,
            "Checkout to $branchName locally",
            true,
        ) {
            override fun run(indicator: ProgressIndicator) {
                switchAction(branchName, force, indicator) {
                    onReady?.run()
                    LOG.info("Switched to a branch '$branchName' locally")
                }
            }
        })
    }

    private fun switchAction(
        branchName: String,
        force: Boolean,
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
        indicator.text2 = "Fetch $branchName from ${Env.data.serverName}"
        val cmd = "git pull ${Env.data.serverName} $branchName"
        val output = CommandRunner.runLocally(myProject, cmd)

        indicator.fraction = 0.5

        if (output.exitCode == 0) {
            localSwitch(branchName, indicator, onReady)
            return
        }

        if (LostConnectionHandler.handle(myProject, output) {
                switchActionIfBranchNotExist(branchName, indicator, onReady)
            }) return
    }
}
