package com.vk.admstorm.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.env.Env

@Service
class HastebinService() {
    companion object {
        fun createHaste(project: Project, data: String): String {
            val output = data.replace("\"", "\\\"").replace("$", "\\$")
            return CommandRunner.runRemotely(project, "echo \"$output\" | ${Env.data.pasteBinCommand}").stdout
        }
    }
}
