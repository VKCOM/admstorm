package com.vk.admstorm.configuration.phpunit

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentContainer
import com.intellij.openapi.util.component1
import com.intellij.openapi.util.component2
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.php.lang.PhpLangUtil
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocRef
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.phpunit.PhpUnitExecutionUtil
import com.jetbrains.php.phpunit.PhpUnitTestPattern
import com.jetbrains.php.phpunit.PhpUnitUtil
import com.vk.admstorm.git.sync.SyncChecker
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import java.util.*

open class RemotePhpUnitRerunFailedTestsAction(container: ComponentContainer, props: RemotePhpUnitConsoleProperties) :
    AbstractRerunFailedTestsAction(container) {

    companion object {
        private val LOG = logger<RemotePhpUnitRerunFailedTestsAction>()
    }

    init {
        init(props)
    }

    override fun getRunProfile(environment: ExecutionEnvironment): MyRunProfile? {
        val configuration = myConsoleProperties.configuration
        return if (configuration !is RemotePhpUnitConfiguration) {
            LOG.warn("Expected PHPUnit run-configuration type, got: ${configuration.javaClass}")
            null
        } else {
            RemotePhpUnitRerunProfile(configuration, getFailedTestPatterns(configuration.project))
        }
    }

    private fun getFailedTestPatterns(project: Project): List<PhpUnitTestPattern?> {
        val failedTests = getFailedTests(project)

        val result: MutableMap<String?, MutableList<PhpUnitTestPattern>?> = HashMap()

        val iterator = failedTests.iterator()
        while (true) {
            var testProxy: AbstractTestProxy
            var parent: AbstractTestProxy?

            do {
                do {
                    if (!iterator.hasNext()) {
                        return result.values.filterNotNull().flatten()
                    }
                    testProxy = iterator.next() as AbstractTestProxy
                } while (!testProxy.isLeaf)
                parent = testProxy.parent
            } while (parent == null)

            val testName = testProxy.name
            val isFakeWarningTest = StringUtil.equals(testName, "Warning")
            if (isFakeWarningTest && !result.containsKey("Warning")) {
                result["Warning"] = ContainerUtil.emptyList()
            }

            val parentLocation = parent.getLocation(project, myConsoleProperties.scope)
            val element = parentLocation?.psiElement
            if (element == null) {
                LOG.warn("Element is null for test location")
            }

            val testFilePath = element?.containingFile?.virtualFile?.path ?: ""

            var className: String?
            var fullTestName: String

            if (element is Method) {
                val grandParent = parent.parent
                className = grandParent.name
                fullTestName = if (isFakeWarningTest) parent.name + "(.*)?" else testName
            } else if (parent.children.size == 1 && isFakeWarningTest) {
                className = parent.name + "(.*)?"
                fullTestName = ""
            } else {
                className = parent.name
                fullTestName = testName
            }

            var dataSet: String? = null
            if (fullTestName.contains(" with data set ")) {
                val methodNameAndDataSet = fullTestName.split(" with data set ")
                if (methodNameAndDataSet.size >= 2) {
                    fullTestName = methodNameAndDataSet[0]
                    dataSet = methodNameAndDataSet[1]
                } else {
                    LOG.warn("Invalid PHPUnit test name: $fullTestName")
                }
            }

            fullTestName = StringUtil.escapeBackSlashes(fullTestName)

            val pattern = PhpUnitTestPattern(className!!, fullTestName, testFilePath, dataSet)
            if (fullTestName.isEmpty()) {
                if (!result.containsKey(className)) {
                    result[className] = ContainerUtil.emptyList()
                }
            } else if (!result.containsKey(className)) {
                result[className] = ContainerUtil.newArrayList(pattern)
            } else {
                (result[className] as MutableList).add(pattern)
            }
        }
    }

    protected class RemotePhpUnitRerunProfile(
        private val conf: RemotePhpUnitConfiguration,
        private val failed: List<PhpUnitTestPattern?>
    ) : MyRunProfile(conf) {

        private fun doCheckSync() {
            SyncChecker.getInstance(conf.project).doCheckSyncSilentlyTask({
                onCanceledSync()
            }) {}
        }

        private fun onCanceledSync() {
            AdmWarningNotification("Current launch may not be correct due to out of sync")
                .withTitle("Launch on out of sync")
                .withActions(
                    AdmNotification.Action("Synchronize...") { _, notification ->
                        notification.expire()
                        SyncChecker.getInstance(conf.project).doCheckSyncSilentlyTask({}, {})
                    }
                )
                .show()
        }

        override fun getState(exec: Executor, env: ExecutionEnvironment): RunProfileState {
            return RunProfileState { executor, _ ->
                ApplicationManager.getApplication().invokeAndWait {
                    doCheckSync()
                }

                val command = buildCommand(env)
                RemotePhpUnitConfigurationRunState.executeRemotePhpUnitCommand(
                    executor,
                    command,
                    env,
                    conf
                )
            }
        }

        private fun buildCommand(env: ExecutionEnvironment): String {
            val filterArgument = createFilterArgument(env.project, failed)
            val filter = "--filter '/$filterArgument$/'"

            return RemotePhpUnitConfigurationRunState.buildCommand(env, conf, filter)
        }

        private fun createFilterArgument(project: Project, patterns: List<PhpUnitTestPattern?>): String {
            val patternsWithDependencies = patterns.toMutableSet()

            val iterator: Iterator<*> = patterns.iterator()
            while (true) {
                var method: Method?
                var containingClass: PhpClass?
                do {
                    if (!iterator.hasNext()) {
                        return PhpUnitExecutionUtil.concatenateTests(patternsWithDependencies)
                    }
                    val pattern = iterator.next() as PhpUnitTestPattern
                    method = PhpUnitExecutionUtil.findMethod(
                        project,
                        pattern.containingFileAbsolutePath,
                        pattern.classFqn,
                        pattern.methodName
                    )
                    containingClass = method?.containingClass
                } while (containingClass == null)

                val methodDependencies = collectMethodDependencies(method!!, containingClass)

                methodDependencies.forEach {
                    if (it == null) return@forEach
                    patternsWithDependencies.add(PhpUnitTestPattern.create(it))
                }
            }
        }

        private fun collectMethodDependencies(testMethod: Method, testClass: PhpClass): Collection<Method?> {
            val testNamespace = testClass.namespaceName
            val testClassFQN = testClass.fqn
            val dependencies = ArrayDeque<Method>()
            dependencies.addLast(testMethod)

            val visited = HashSet<Method?>()
            while (true) {
                var currentMethod: Method?
                do {
                    if (dependencies.isEmpty()) {
                        visited.remove(testMethod)
                        return visited
                    }
                    currentMethod = dependencies.pollFirst()
                } while (!visited.add(currentMethod))

                val docComment = currentMethod!!.docComment
                val tags = docComment?.getTagElementsByName("@depends") ?: PhpDocTag.EMPTY_ARRAY

                tags.forEach { tag ->
                    val dependencyPair = PhpUnitUtil.getClassFqnAndMethodName(
                        PsiTreeUtil.getChildOfType(tag, PhpDocRef::class.java), testNamespace
                    )
                    val (dependencyClassFQN, dependencyMethodName) = dependencyPair

                    if (PhpLangUtil.compareFQN(dependencyClassFQN, testClassFQN) == 0) {
                        val ownMethod = testClass.findOwnMethodByName(dependencyMethodName)
                        if (ownMethod != null) {
                            dependencies.add(ownMethod)
                        }
                    }
                }
            }
        }
    }
}
