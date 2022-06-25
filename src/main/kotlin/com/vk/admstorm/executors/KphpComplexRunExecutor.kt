package com.vk.admstorm.executors

import com.intellij.openapi.project.Project
import com.vk.admstorm.configuration.kphp.KphpRunType
import com.vk.admstorm.ui.MyIcons

class KphpComplexRunExecutor(
    project: Project,
    private val type: KphpRunType,
    private val command: String
) : BaseRemoteExecutor(project, "KPHP ${type.command}") {

    override fun layoutName() = "KPHP ${type.command}"

    override fun tabName() = "KPHP ${type.command}"

    override fun command() = command

    override fun icon() = MyIcons.kphp

    override fun onFinish() {}
}
