package com.vk.admstorm.configuration.phpunit

import com.intellij.coverage.CoverageDataManager
import com.intellij.coverage.CoverageExecutor
import com.intellij.coverage.CoverageSuitesBundle
import com.intellij.coverage.DefaultCoverageFileProvider
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.jetbrains.php.phpunit.coverage.PhpUnitCoverageRunner
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.configuration.php.PhpDebugUtils
import com.vk.admstorm.env.Env
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MyPathUtils.remotePathByLocalPath
import com.vk.admstorm.utils.MyPathUtils.resolveProjectDir
import com.vk.admstorm.utils.MySshUtils
import com.vk.admstorm.utils.MyUtils.executeOnPooledThread
import java.io.File

class RemotePhpUnitConfigurationRunState(
    private val env: ExecutionEnvironment,
    private val conf: RemotePhpUnitConfiguration
) : RunProfileState {

    companion object {
        private const val COVERAGE_FILE_NAME = "coverage.xml"

        fun executeRemotePhpUnitCommand(
            exec: Executor?,
            command: String,
            env: ExecutionEnvironment,
            runConfig: RemotePhpUnitConfiguration,
            listener: ProcessListener? = null,
        ): DefaultExecutionResult? {
            if (!SshConnectionService.getInstance(env.project).isConnectedOrWarning()) {
                return null
            }

            if (exec == null) {
                return null
            }

            val workingDir =
                if (runConfig.isApiTest) "${Env.data.projectRoot}/tests/api"
                else "${Env.data.projectRoot}/${Env.data.phpSourceFolder}"

            val consoleProperties = RemotePhpUnitConsoleProperties(runConfig, exec)
            val console = SMTestRunnerConnectionUtil
                .createConsole(
                    consoleProperties.testFrameworkName,
                    consoleProperties
                ) as SMTRunnerConsoleView

            val fullCommand = "cd $workingDir && $command"
            val handler = MySshUtils.exec(env.project, fullCommand) ?: return null

            console.attachToProcess(handler)

            if (listener != null) {
                handler.addProcessListener(listener)
            }

            val smTestProxy = console.resultsViewer.root as SMTestProxy.SMRootTestProxy
            smTestProxy.setTestsReporterAttached()
            smTestProxy.setSuiteStarted()

            val result = DefaultExecutionResult(console, handler)
            val rerunFailedTestsAction = RemotePhpUnitRerunFailedTestsAction(console, consoleProperties)
            rerunFailedTestsAction.setModelProvider { console.resultsViewer }
            result.setRestartActions(rerunFailedTestsAction)

            return result
        }

        private fun getClassNameFromFQN(name: String): String {
            if (!name.contains("\\")) {
                return name
            }

            return name.substring(name.lastIndexOf('\\') + 1 until name.length)
        }

        fun buildCommand(
            env: ExecutionEnvironment,
            conf: RemotePhpUnitConfiguration,
            withCoverage: Boolean = false,
            filter: String = ""
        ): String {
            val phpUnitXml = remotePathByLocalPath(env.project, conf.phpUnitConfig)
            val phpUnitExe = remotePathByLocalPath(env.project, conf.phpUnitExe)

            val additional = conf.additionalParameters
            val coverageEnv = if (withCoverage) "XDEBUG_MODE=coverage " else ""
            val coverageFlag = if (withCoverage) " --coverage-clover $COVERAGE_FILE_NAME" else ""

            val base = "$coverageEnv$phpUnitExe$coverageFlag --teamcity --configuration $phpUnitXml $additional $filter"

            if (conf.scope == PhpUnitScope.Directory) {
                val remoteDir = remotePathByLocalPath(env.project, conf.directory)
                return "$base $remoteDir"
            }

            if (conf.scope == PhpUnitScope.Class || conf.scope == PhpUnitScope.Method) {
                val className = getClassNameFromFQN(conf.className)
                val localDir = File(conf.filename).parentFile.path ?: ""
                val localFile = File(conf.filename).name
                val remoteDir = remotePathByLocalPath(env.project, localDir)

                val classFilter = if (filter.isNotEmpty()) {
                    "" // not set other filter
                } else if (conf.scope == PhpUnitScope.Method) {
                    "--filter $className::${conf.methodName}"
                } else {
                    "--filter $className"
                }

                return "$base $classFilter --test-suffix $localFile $remoteDir"
            }

            return base
        }
    }

    override fun execute(exec: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        if (exec is DefaultDebugExecutor) {
            return runPhpUnitDebug(exec)
        }
        if (exec is CoverageExecutor) {
            return runPhpUnit(exec, withCoverage = true, object : ProcessListener {
                override fun processTerminated(event: ProcessEvent) {
                    showCoverage()
                }
                override fun startNotified(event: ProcessEvent) {}
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {}
            })
        }

        return runPhpUnit(exec)
    }

    private fun showCoverage() {
        executeOnPooledThread {
            val text = CommandRunner.runRemotely(
                env.project,
                "cd ${MyPathUtils.resolveRemoteRoot()}/${Env.data.phpSourceFolder} && cat $COVERAGE_FILE_NAME"
            ).stdout

            val projectDir = env.project.resolveProjectDir() ?: return@executeOnPooledThread
            val path = Env.data.kphpRelatedPathBegin + "/" + MyPathUtils.remoteUserName() + "/data/"
            val fixedText = text.replace(path, "$projectDir/")

            val file = File(projectDir, COVERAGE_FILE_NAME)
            if (!file.exists()) {
                file.createNewFile()
            }

            file.writeText(fixedText)

            val virtualFile =
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file) ?: return@executeOnPooledThread

            invokeLater {
                val manager = CoverageDataManager.getInstance(env.project)
                val coverageSuite = manager
                    .addExternalCoverageSuite(
                        virtualFile.name, virtualFile.timeStamp, PhpUnitCoverageRunner(),
                        DefaultCoverageFileProvider(file.path)
                    )

                manager.chooseSuitesBundle(CoverageSuitesBundle(coverageSuite))
            }
        }
    }

    private fun runPhpUnitDebug(exec: Executor?): ExecutionResult? {
        val project = env.project
        PhpDebugUtils.enablePhpDebug(project)
        val ok = PhpDebugUtils.checkSshTunnel(project) { runPhpUnitDebug(exec) }
        if (!ok) {
            return null
        }

        PhpDebugUtils.showNotificationAboutDelay(project)
        val command = buildCommand(env, conf).removePrefix("vk ")
        val commandWithoutPhpUnit = command.substring(command.indexOf(' ') + 1)
        val fullCommand = "source ~/.php_debug && php_debug_test $commandWithoutPhpUnit"

        return executeRemotePhpUnitCommand(exec, fullCommand, env, conf)
    }

    private fun runPhpUnit(
        exec: Executor?,
        withCoverage: Boolean = false,
        listener: ProcessListener? = null
    ): ExecutionResult? {
        return executeRemotePhpUnitCommand(exec, buildCommand(env, conf, withCoverage), env, conf, listener)
    }
}
