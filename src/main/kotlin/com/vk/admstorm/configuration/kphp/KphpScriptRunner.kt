package com.vk.admstorm.configuration.kphp

import com.intellij.execution.ExecutionResult
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.vk.admstorm.env.Env
import com.vk.admstorm.executors.KphpScriptExecutor
import com.vk.admstorm.utils.MyPathUtils

class KphpScriptRunner(
    private val project: Project,
    private val myRunConfiguration: KphpConfiguration
) {
    companion object {
        fun buildCommand(project: Project, scriptPath: String): String {
            val wwwBasedScriptPath = "${Env.data.phpSourceFolder}/$scriptPath"
            val absoluteScriptPath = MyPathUtils.absoluteDataBasedRemotePath(project, wwwBasedScriptPath)
            val remoteRoot = MyPathUtils.resolveRemoteRoot(project)
            val scriptOutput = KphpUtils.scriptBinaryPath(project)
            val includeDirsFlag = KphpUtils.includeDirsAsFlags(project)

            return Env.data.kphp2cpp +
                    " $includeDirsFlag" +
                    " -o $scriptOutput" +
                    " --composer-root $remoteRoot" +
                    " --mode cli" +
                    " $absoluteScriptPath"
        }
    }

    fun run(): ExecutionResult? {
        val command = buildCommand(project, myRunConfiguration.parameters)
        val executor = KphpScriptExecutor(project, command, myRunConfiguration)

        executor.withPhpOutputHandler { output, console ->
            console.clear()

            console.printlnSystem("php ${Env.data.projectRoot}/${Env.data.phpSourceFolder}/${myRunConfiguration.parameters}")
            console.println()

            console.println(output.stdout.ifEmpty { "<no output>" })

            if (output.stdout.contains("Class") && output.stdout.contains("not found")) {
                console.println()
                console.println(
                    "It looks like PHP didn't find the class, try including autoload:\n\n" +
                            "#ifndef KPHP\n" +
                            "require_once __DIR__ . '/../vendor/autoload.php';\n" +
                            "require_once 'autoload.php';\n" +
                            "#endif"
                )
            }

            val stderr = output.stderr.trim()
            if (stderr.isNotEmpty()) {
                console.println()
                console.printlnError(stderr)
            }
        }

        executor.withKphpOutputHandler { output, console ->
            console.clear()

            console.printlnSystem(KphpUtils.scriptBinaryPath(project))
            console.println()

            console.println(output.stdout.ifEmpty { "<no output>" })

            val stderr = output.stderr.trim()
            if (stderr.isNotEmpty()) {
                console.println()
                console.printlnError(output.stderr.trim())
            }
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            executor.run()
        }
        return null
    }
}
