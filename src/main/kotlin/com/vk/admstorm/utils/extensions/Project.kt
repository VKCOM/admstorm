package com.vk.admstorm.utils.extensions

import com.intellij.openapi.project.Project
import com.vk.admstorm.AdmService

fun Project.pluginEnabled() = AdmService.getInstance(this).needBeEnabled()
