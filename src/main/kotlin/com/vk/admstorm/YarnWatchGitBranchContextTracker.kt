package com.vk.admstorm

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.BranchChangeListener

class YarnWatchGitBranchContextTracker(project: Project) : BranchChangeListener {
    companion object {
        private val LOG = logger<YarnWatchGitBranchContextTracker>()
    }

    private val service = YarnWatchService.getInstance(project)

    override fun branchWillChange(branchName: String) {
        LOG.info("Branch will change: $branchName, yarn watch state: ${service.state()}")

        if (service.isRunning()) {
            service.stop()
            LOG.info("Yarn watch stopped because branch will change")
        }
    }

    override fun branchHasChanged(branchName: String) {}
}
