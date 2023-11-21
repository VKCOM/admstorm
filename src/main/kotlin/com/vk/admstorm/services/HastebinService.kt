package com.vk.admstorm.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.env.Env

@Service(Service.Level.PROJECT)
class HastebinService constructor(private val project: Project) {
    companion object {
        fun getInstance(project: Project) = project.service<HastebinService>()
    }
    fun createHaste(data: String): String {
        val output = data.replace("\"", "\\\"").replace("$", "\\$")
        return CommandRunner.runRemotely(project, "echo \"$output\" | ${Env.data.pasteBinCommand}").stdout
    }
}
