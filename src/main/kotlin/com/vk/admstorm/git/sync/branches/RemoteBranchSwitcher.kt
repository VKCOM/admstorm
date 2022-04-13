package com.vk.admstorm.git.sync.branches

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsNotifier
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.env.Env
import com.vk.admstorm.git.GitUtils
import com.vk.admstorm.git.GitUtils.remoteStashAndAction
import com.vk.admstorm.git.sync.conflicts.GitCheckoutProblemsHandler
import com.vk.admstorm.git.sync.conflicts.GitProblemsHandlerBase
import com.vk.admstorm.ssh.LostConnectionHandler
import com.vk.admstorm.utils.MyUtils.runBackground
import com.vk.admstorm.utils.ServerNameProvider
import git4idea.GitNotificationIdsHolder
import git4idea.branch.GitBranchUtil
import git4idea.util.GitUIUtil.bold
import git4idea.util.GitUIUtil.code

class RemoteBranchSwitcher(private val myProject: Project, private val myOnGitConflictCanceled: Runnable? = null) {
    companion object {
        private val LOG = Logger.getInstance(RemoteBranchSwitcher::class.java)
    }

    private fun command(branchName: String, force: Boolean): String {
        val forceFlag = if (force) " --force" else ""

        return "git checkout $branchName$forceFlag"
    }

    fun switch(branchName: String, force: Boolean, onReady: Runnable? = null) {
        if (!Env.isResolved()) {
            LOG.warn("Env not resolved yet")
            return
        }

        LOG.info("Start switch action to '$branchName' (force: $force) on ${ServerNameProvider.name()}")
        doSwitchTask(branchName, force, onReady)
    }

    private fun doSwitchTask(branchName: String, force: Boolean, onReady: Runnable? = null) {
        runBackground(myProject, "Checkout to $branchName on ${ServerNameProvider.name()}") {
            switchAction(branchName, force, onReady)
        }
    }

    private fun switchAction(branch: String, force: Boolean, onReady: Runnable?) {
        val branchName = if (branch == "HEAD") {
            GitBranchUtil.getCurrentRepository(myProject)?.currentBranch?.name ?: return
        } else branch

        if (!GitUtils.remoteBranchExist(myProject, branchName)) {
            LOG.info("Branch '$branchName' doesn't exist on ${ServerNameProvider.name()}")

            // If the branch does not exist, then we must first push
            // the local branch to server, in order to then switch to it.
            switchActionIfBranchNotExist(branchName, onReady)
            return
        }

        val output = CommandRunner.runRemotely(myProject, command(branchName, force))

        if (output.exitCode == 0) {
            showSuccessMessage(branchName)
            onReady?.run()
            return
        }

        val state = GitCheckoutProblemsHandler(myProject).handle(output, {
            doStashAndCheckout(branchName, onReady)
        }) {
            doForceCheckout(branchName, onReady)
        }

        if (state == GitProblemsHandlerBase.State.Canceled) {
            myOnGitConflictCanceled?.run()
        }
    }

    private fun showSuccessMessage(branchName: String) {
        VcsNotifier.getInstance(myProject).notifySuccess(
            GitNotificationIdsHolder.CHECKOUT_SUCCESS + ".${ServerNameProvider.name()}", "",
            "Checked out ${bold(code(branchName))} on ${ServerNameProvider.name()}",
            null
        )
        LOG.info("Successfully switched to a branch '$branchName' on ${ServerNameProvider.name()}")
    }

    private fun switchActionIfBranchNotExist(branchName: String, onReady: Runnable?) {
        val cmd = "git push --set-upstream ${ServerNameProvider.name()} $branchName"
        val output = CommandRunner.runLocally(myProject, cmd)

        if (output.exitCode == 0) {
            LOG.info("Local push '$branchName' with set-upstream to ${ServerNameProvider.name()} completed successfully")
            LOG.info("Started checkout on this branch")

            switch(branchName, false, onReady)
            showSuccessMessage(branchName)
            return
        }

        if (LostConnectionHandler.handle(myProject, output) {
                switchActionIfBranchNotExist(branchName, onReady)
            }) return

        GitCheckoutProblemsHandler(myProject).handle(output, {
            doStashAndCheckout(branchName, onReady)
        }) {
            doForceCheckout(branchName, onReady)
        }
    }

    private fun doForceCheckout(branchName: String, onReady: Runnable?) {
        switch(branchName, true, onReady)
    }

    private fun doStashAndCheckout(branchName: String, onReady: Runnable?) {
        remoteStashAndAction(myProject) {
            switchAction(branchName, false, onReady)
        }
    }
}
