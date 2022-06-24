package com.vk.admstorm.executors

import com.intellij.execution.Output
import com.intellij.execution.OutputListener
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.application.ApplicationManager
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
import java.util.function.BiConsumer
import javax.swing.Icon

class KphpScriptExecutor(project: Project, command: String, private val myRunConfiguration: KphpConfiguration) :
    BaseRunnableExecutor(Config(tabName = "KPHP Script", command = command, workingDir = "~/"), project) {

    private val myKphpOutputTab = ConsoleTab(project, "KPHP Output")
    private val myPhpOutputTab = ConsoleTab(project, "PHP Output")
    private val myDiffTab = DiffTab(project, "Output Diff")

    private var myPhpOutputHandler: BiConsumer<Output, Console> = BiConsumer { _, _ -> }
    private var myKphpOutputHandler: BiConsumer<Output, Console> = BiConsumer { _, _ -> }

    init {
        withTab(myKphpOutputTab)

        if (myRunConfiguration.runScriptWithPhp) {
            withTab(myPhpOutputTab)
            withTab(myDiffTab)
        }
    }

    fun withPhpOutputHandler(handler: BiConsumer<Output, Console>) {
        myPhpOutputHandler = handler
    }

    fun withKphpOutputHandler(handler: BiConsumer<Output, Console>) {
        myKphpOutputHandler = handler
    }

    override fun onFinish() {
        val output = output()

        myKphpOutputTab.console.clear()

        invokeLater {
            if (output.exitCode != 0) {
                myKphpOutputTab.content?.displayName = "Compilation Errors"
                myKphpOutputTab.console.println(KphpScriptOutputParser.parse(output))
                myKphpOutputTab.console.view().scrollTo(0)
            }

            selectTab(myKphpOutputTab)
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            if (output.exitCode == 0) {
                executeKphpScriptBinary()
            }
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            executePhpScript()
        }
    }

    override fun icon(): Icon = MyIcons.kphp

    private fun executePhpScript() {
        if (!myRunConfiguration.runScriptWithPhp) {
            return
        }

        val command = "php ${Env.data.projectRoot}/${Env.data.phpSourceFolder}/${myRunConfiguration.parameters}"
        val handler = MySshUtils.exec(
            project, command,
            "php ${Env.data.projectRoot}/${Env.data.phpSourceFolder}/${myRunConfiguration.parameters}\n"
        ) ?: return

        myPhpOutputTab.console.view().attachToProcess(handler)
        myPhpOutputTab.console.clear()

        handler.addProcessListener(object : OutputListener() {
            override fun processTerminated(event: ProcessEvent) {
                super.processTerminated(event)
                myDiffTab.viewer.withPhpOutput(output.stdout.ifEmpty { "<no output>" })

                myPhpOutputHandler.accept(output, myPhpOutputTab.console)

                myPhpOutputTab.console.view().scrollTo(0)
            }
        })

        handler.startNotify()
    }

    private fun executeKphpScriptBinary() {
        val scriptOutput = KphpUtils.scriptBinaryPath(project)
        val command = "$scriptOutput --Xkphp-options --disable-sql"
        val handler = MySshUtils.exec(project, command, "$scriptOutput\n") ?: return

        myKphpOutputTab.console.view().attachToProcess(handler)
        myKphpOutputTab.console.clear()

        handler.addProcessListener(object : OutputListener() {
            override fun processTerminated(event: ProcessEvent) {
                super.processTerminated(event)
                myDiffTab.viewer.withKphpOutput(output.stdout.ifEmpty { "<no output>" })

                myKphpOutputHandler.accept(output, myKphpOutputTab.console)

                myKphpOutputTab.console.view().scrollTo(0)
            }
        })

        handler.startNotify()
    }
}
