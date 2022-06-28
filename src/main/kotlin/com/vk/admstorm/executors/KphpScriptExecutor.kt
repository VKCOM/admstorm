package com.vk.admstorm.executors

import com.intellij.execution.Output
import com.intellij.execution.OutputListener
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.vk.admstorm.configuration.kphp.KphpConfiguration
import com.vk.admstorm.configuration.kphp.KphpUtils
import com.vk.admstorm.console.Console
import com.vk.admstorm.env.Env
import com.vk.admstorm.executors.tabs.ConsoleTab
import com.vk.admstorm.executors.tabs.DiffTab
import com.vk.admstorm.parsers.KphpScriptOutputParser
import com.vk.admstorm.ui.MyIcons
import com.vk.admstorm.utils.MySshUtils
import com.vk.admstorm.utils.MyUtils.executeOnPooledThread
import java.util.function.BiConsumer

class KphpScriptExecutor(
    project: Project,
    private val command: String,
    private val conf: KphpConfiguration
) : BaseRemoteExecutor(project, "KPHP Script") {

    private val kphpOutputTab = ConsoleTab(project, "KPHP Output")
    private val phpOutputTab = ConsoleTab(project, "PHP Output")
    private val diffTab = DiffTab(project, "Output Diff")

    private var phpOutputHandler: BiConsumer<Output, Console> = BiConsumer { _, _ -> }
    private var kphpOutputHandler: BiConsumer<Output, Console> = BiConsumer { _, _ -> }

    init {
        withTab(kphpOutputTab)

        if (conf.runScriptWithPhp) {
            withTab(phpOutputTab)
            withTab(diffTab)
        }
    }

    override fun layoutName() = "KPHP Script"

    override fun tabName() = "KPHP Script"

    override fun command() = command

    override fun workingDir() = "~/"

    override fun icon() = MyIcons.kphp

    override fun onFinish() {
        val output = output()

        kphpOutputTab.console.clear()

        invokeLater {
            if (output.exitCode != 0) {
                kphpOutputTab.content?.displayName = "Compilation Errors"
                kphpOutputTab.console.println(KphpScriptOutputParser.parse(output))
                kphpOutputTab.console.view().scrollTo(0)
            }

            selectTab(kphpOutputTab)
        }

        executeOnPooledThread {
            if (output.exitCode == 0) {
                executeKphpScriptBinary()
            }
        }

        executeOnPooledThread {
            executePhpScript()
        }
    }

    fun withPhpOutputHandler(handler: BiConsumer<Output, Console>) {
        phpOutputHandler = handler
    }

    fun withKphpOutputHandler(handler: BiConsumer<Output, Console>) {
        kphpOutputHandler = handler
    }

    private fun executePhpScript() {
        if (!conf.runScriptWithPhp) {
            return
        }

        val command = "php ${Env.data.projectRoot}/${Env.data.phpSourceFolder}/${conf.parameters}"
        val handler = MySshUtils.exec(
            project, command,
            "php ${Env.data.projectRoot}/${Env.data.phpSourceFolder}/${conf.parameters}\n"
        ) ?: return

        phpOutputTab.console.view().attachToProcess(handler)
        phpOutputTab.console.clear()

        handler.addProcessListener(object : OutputListener() {
            override fun processTerminated(event: ProcessEvent) {
                super.processTerminated(event)
                diffTab.viewer.withPhpOutput(output.stdout.ifEmpty { "<no output>" })

                phpOutputHandler.accept(output, phpOutputTab.console)

                phpOutputTab.console.view().scrollTo(0)
            }
        })

        handler.startNotify()
    }

    private fun executeKphpScriptBinary() {
        val scriptOutput = KphpUtils.scriptBinaryPath(project)
        val command = "$scriptOutput --Xkphp-options --disable-sql"
        val handler = MySshUtils.exec(project, command, "$scriptOutput\n") ?: return

        kphpOutputTab.console.view().attachToProcess(handler)
        kphpOutputTab.console.clear()

        handler.addProcessListener(object : OutputListener() {
            override fun processTerminated(event: ProcessEvent) {
                super.processTerminated(event)
                diffTab.viewer.withKphpOutput(output.stdout.ifEmpty { "<no output>" })

                kphpOutputHandler.accept(output, kphpOutputTab.console)

                kphpOutputTab.console.view().scrollTo(0)
            }
        })

        handler.startNotify()
    }
}
