package com.vk.admstorm.executors

import com.intellij.openapi.project.Project
import com.vk.admstorm.configuration.kphp.KphpRunType
import com.vk.admstorm.ui.MyIcons
import javax.swing.Icon

class KphpComplexRunExecutor(project: Project, type: KphpRunType, command: String) :
    BaseRunnableExecutor(Config(tabName = "KPHP ${type.command}", command = command), project) {

    override fun onReady() {}

    override fun icon(): Icon = MyIcons.kphp
}
