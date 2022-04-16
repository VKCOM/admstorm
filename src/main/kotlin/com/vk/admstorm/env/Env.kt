package com.vk.admstorm.env

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.configuration.kphp.KphpRunType
import com.vk.admstorm.configuration.phplinter.PhpLinterCheckers
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.ui.MessageDialog
import com.vk.admstorm.utils.MyUtils.measureTime
import git4idea.util.GitUIUtil.bold
import git4idea.util.GitUIUtil.code
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

// See https://github.com/Kotlin/kotlinx.serialization/issues/993
@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class KphpCommand(
    var key: String,
    var command: String,
    var arguments: String,
    var description: String
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class EnvConfig(
    var serverName: String = "",
    var projectRoot: String = "",
    var phpSourceFolder: String = "",
    var kphpRelatedPathBegin: String = "",
    var kphpRelativeIncludeDirs: List<String> = listOf(),
    var vkCommand: String = "",
    var kphpCommand: String = "",
    var kphp2cpp: String = "",
    var kphpScriptBinaryPath: String = "",
    var phpLinterCommand: String = "",
    var ktestCommand: String = "",
    var kphpInspectorCommand: String = "",
    var phpunitCommand: String = "",
    var pasteBinCommand: String = "",
    var debugLogUrl: String = "",
    var debugLogFqns: List<String> = listOf(),
    var syncScriptCommand: String = "",
    var kphpCommands: List<KphpCommand> = listOf()
)

object Env {
    private val LOG = Logger.getInstance(Env::class.java)

    private var myIsResolved = false
    var data = EnvConfig()

    private fun checkPropertyNotEmpty(name: String, value: String) =
        value.ifEmpty { throw IllegalArgumentException(name) }

    private fun checkPropertyNotEmpty(name: String, value: List<String>) =
        value.ifEmpty { throw IllegalArgumentException(name) }

    private fun setKphpRunType(command: KphpCommand) {
        val enumConstant = KphpRunType.values().find { it.name == command.key } ?: return
        enumConstant.command = command.command
        enumConstant.arguments = command.arguments
        enumConstant.description = command.description
    }

    fun isResolved() = myIsResolved

    fun resolve(project: Project) {
        if (myIsResolved) {
            LOG.info("Env resolving is skipped because it has already been resolved")
            return
        }

        val configPath = "~/admstorm_config.json"
        val output = CommandRunner.runRemotely(project, "cat $configPath")
        if (output.exitCode != 0) {
            MessageDialog.showError(
                """
                    Unable to get ${code("admstorm_config.json")}, further work of the plugin is impossible
                    
                    ${output.stderr}
                """.trimIndent(),
                "Problem with AdmStorm env data"
            )
            return
        }

        val format = Json { ignoreUnknownKeys = true }
        data = try {
            format.decodeFromString(output.stdout)
        } catch (e: Exception) {
            MessageDialog.showError(
                """
                    Exception while deserialize data, further work of the plugin is impossible
                    
                    ${e.message}
                """.trimIndent(),
                "Problem with AdmStorm env data"
            )

            LOG.warn("Exception while deserialize data, further work of the plugin is impossible:", e)
            return
        }

        try {
            data.kphpCommands.forEach {
                setKphpRunType(it)
            }

            checkPropertyNotEmpty("serverName", data.serverName)
            checkPropertyNotEmpty("projectRoot", data.projectRoot)
            checkPropertyNotEmpty("phpSourceFolder", data.phpSourceFolder)
            checkPropertyNotEmpty("kphpRelatedPathBegin", data.kphpRelatedPathBegin)
            checkPropertyNotEmpty("kphpRelativeIncludeDirs", data.kphpRelativeIncludeDirs)
            checkPropertyNotEmpty("kphpCommand", data.kphpCommand)
            checkPropertyNotEmpty("kphp2cpp", data.kphp2cpp)
            checkPropertyNotEmpty("kphpScriptBinaryPath", data.kphpScriptBinaryPath)
            checkPropertyNotEmpty("phpLinterCommand", data.phpLinterCommand)
            checkPropertyNotEmpty("ktestCommand", data.ktestCommand)
            checkPropertyNotEmpty("kphpInspectorCommand", data.kphpInspectorCommand)
            checkPropertyNotEmpty("phpunitCommand", data.phpunitCommand)
            checkPropertyNotEmpty("pasteBinCommand", data.pasteBinCommand)
            checkPropertyNotEmpty("debugLogUrl", data.debugLogUrl)
            checkPropertyNotEmpty("debugLogFqns", data.debugLogFqns)
            checkPropertyNotEmpty("syncScriptCommand", data.syncScriptCommand)
        } catch (e: IllegalArgumentException) {
            LOG.warn("Lost ${e.message} argument, further work of the plugin maybe incomplete")
        }

        measureTime(LOG, "check remote server exist") {
            checkRemoteServerGitRemoteExist(project)
        }

        measureTime(LOG, "set `git config receive.denyCurrentBranch ignore`") {
            setReceiveDenyCurrentBranch(project)
        }

        measureTime(LOG, "set PHP Linter checkers") {
            setPhpLinterCheckers(project)
        }

        myIsResolved = true
    }

    private fun setPhpLinterCheckers(project: Project) {
        val outputCheckers = CommandRunner.runRemotely(project, "${data.phpLinterCommand} checkers-doc")
        if (outputCheckers.exitCode != 0) {
            MessageDialog.showError(
                """
                    Unable to get PHP Linter checkers, further work of the plugin maybe incomplete
                    
                    ${outputCheckers.stderr}
                    """.trimIndent(),
                "Problem with PHP Linter checkers doc"
            )
            return
        }

        val parts = outputCheckers.stdout.split("\n### ")
        parts.forEach { part ->
            if (!part.startsWith("`")) return@forEach
            val name = part.substring(1 until part.indexOf(" ") - 1)

            val markdown = "## PHP Linter " + part.split("\n").mapNotNull { line ->
                if (line.startsWith(">")) return@mapNotNull null
                if (line.startsWith("#### Description")) return@mapNotNull null
                if (line.startsWith("####")) return@mapNotNull line.removePrefix("#")
                if (line.startsWith("`") && !line.startsWith("```")) return@mapNotNull line.replace("`", "'")

                line
            }.joinToString("\n")

            val flavour = CommonMarkFlavourDescriptor()
            val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdown)
            val html = HtmlGenerator(markdown, parsedTree, flavour).generateHtml()

            PhpLinterCheckers.nameToCheckerDoc[name] = html
        }
    }

    private fun checkRemoteServerGitRemoteExist(project: Project) {
        val output = CommandRunner.runLocally(project, "git remote get-url ${data.serverName}")
        if (output.exitCode != 0) {
            AdmNotification(
                """
                    A git remote named ${bold(data.serverName)}, which is required for the plugin to work, was not found.
                    <br>
                    It will be created automatically
                """.trimIndent()
            )
                .show()

            val addCommandOutput =
                CommandRunner.runLocally(
                    project,
                    "git remote add ${data.serverName} ${data.serverName}:${data.projectRoot}"
                )
            if (addCommandOutput.exitCode != 0) {
                AdmWarningNotification(
                    """
                        The new git remote was not created:
                        <br>
                        <br>
                        ${addCommandOutput.stderr}
                    """.trimIndent()
                )
                    .withTitle("Creating git remote failed")
                    .show()
                return
            }

            AdmNotification()
                .withTitle("Added new git remote named ${bold(data.serverName)}")
                .show()
        }
    }

    /**
     * We need to set 'ignore' value for receive.denyCurrentBranch in order to be able to
     * push to the development server, on which the same branch is enabled at the time
     * (since the plugin synchronizes branches, this will almost always be the case),
     * otherwise the push will fail.
     */
    private fun setReceiveDenyCurrentBranch(project: Project) {
        val output = CommandRunner.runRemotely(project, "git config receive.denyCurrentBranch ignore")
        if (output.exitCode != 0) {
            AdmNotification(
                """
                    Failed to set ${code("git config value receive.denyCurrentBranch ignore")}. 
                    Set it manually for the correct plugin work.
                    <br>
                    <br>
                    ${output.stderr}
                """.trimIndent()
            )
                .show()
        }
    }
}
