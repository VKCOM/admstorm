package com.vk.admstorm.actions

import com.intellij.execution.Output
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.PhpFileImpl
import com.jetbrains.php.lang.psi.elements.PhpPsiElement
import com.jetbrains.php.lang.psi.elements.impl.FunctionImpl
import com.jetbrains.php.lang.psi.elements.impl.PhpClassImpl
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.env.Env
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.parsers.InspectKphpOutputParser
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.transfer.TransferService
import com.vk.admstorm.utils.MyEditorUtils
import com.vk.admstorm.utils.MyUtils.runBackground
import com.vk.admstorm.utils.extensions.pluginEnabled
import git4idea.util.GitUIUtil.code

class ShowKphpSourcesAction : PhpPsiElementAction<PhpPsiElement>(PhpPsiElement::class.java) {
    companion object {
        private val LOG = logger<ShowKphpSourcesAction>()
    }

    override val errorHint: String = "Error"
    private var myInitialElementLine = 0

    override fun update(e: AnActionEvent) {
        if (e.project == null || !e.project!!.pluginEnabled()) {
            e.presentation.isEnabledAndVisible = false
        }
    }

    override fun actionPerformed(element: PhpPsiElement) {
        if (!SshConnectionService.getInstance(element.project).isConnectedOrWarning()) {
            return
        }
        actionWithConnectionPerformed(element)
    }

    private fun actionWithConnectionPerformed(element: PhpPsiElement) {
        when (element) {
            is PhpClassImpl -> doSearchClassTask(element)
            is FunctionImpl -> doSearchFunctionTask(element)
            else -> {
                LOG.info("Not a class or function node")

                myInitialElementLine = element.line()
                val parent = findParentClassOrFunction(element)

                LOG.info("Found parent: '${parent?.kphpFqn()}'")

                when (parent) {
                    is PhpClassImpl -> doSearchClassTask(parent)
                    is FunctionImpl -> doSearchFunctionTask(parent)
                    null -> {
                        AdmWarningNotification(
                            """
                                Can't find a class or function nearby
                                <br>
                                Try calling this action inside a class or function
                            """.trimIndent()
                        )
                            .withTitle("C++ sources not found")
                            .show()
                    }
                }
            }
        }
    }

    private fun doSearchFunctionTask(function: FunctionImpl) {
        val fqn = function.kphpFqn()
        val project = function.project

        runBackground(function.project, "Search function $fqn") {
            doSearchFunction(project, fqn)
        }
    }

    private fun doSearchFunction(project: Project, fqn: String) {
        val searchCommand = "${Env.data.kphpInspectorCommand} --debug --f \\\\$fqn"

        val output = CommandRunner.runRemotely(project, searchCommand)

        LOG.info("Inspector output:\n'${output.stdout}'\n'${output.stderr}'")

        if (output.stdout.contains("Multiple files found for query")) {
            return
        }
        if (output.stdout.contains("No function found for query")) {
            AdmWarningNotification(
                "Function ${code(fqn.replace("\\\\", "\\"))} was not compiled by KPHP or cannot be found"
            )
                .withTitle("C++ sources not found")
                .show()
            return
        }

        handleInspectorOutput(project, output)
    }

    private fun doSearchClassTask(klass: PhpClassImpl) {
        val fqn = klass.kphpFqn()
        val project = klass.project

        runBackground(klass.project, "Search class $fqn") {
            doSearchClass(project, fqn)
        }
    }

    private fun doSearchClass(project: Project, fqn: String) {
        val searchCommand = "${Env.data.kphpInspectorCommand} --debug --cl \\$fqn"

        val output = CommandRunner.runRemotely(project, searchCommand)

        LOG.info("Inspector output:\n'${output.stdout}'\n'${output.stderr}'")

        if (output.stdout.contains("Multiple files found for query")) {
            return
        }
        if (output.stdout.contains("No class found for query")) {
            AdmWarningNotification(
                """
                    Class ${code(fqn.replace("\\\\", "\\"))} was not compiled by KPHP or the class has no fields 
                    and KPHP hasn't created a separate file for it
                """.trimIndent()
                    .replace("\n", ""),
            )
                .withTitle("C++ sources not found")
                .show()
            return
        }

        handleInspectorOutput(project, output)
    }

    private fun handleInspectorOutput(
        project: Project,
        output: Output,
    ) {
        val cppSourcesFile = InspectKphpOutputParser.parse(output.stdout)

        LOG.info("Found source filename: '$cppSourcesFile'")

        LOG.info("Start download '$cppSourcesFile'")
        TransferService.getInstance(project).downloadFile(cppSourcesFile) { downloadedFile ->
            MyEditorUtils.openFileInRightTab(project, downloadedFile) { openedEditorWindow, _ ->
                LOG.info("Successfully downloaded")

                if (myInitialElementLine == 0) {
                    return@openFileInRightTab
                }

                MyEditorUtils.applyLastEditor<PsiAwareTextEditorImpl>(openedEditorWindow) {
                    MyEditorUtils.scrollToLineWithShift(it, 1) { line ->
                        if (line.startsWith("//$myInitialElementLine:")) {
                            LOG.info("Find needed line $myInitialElementLine in file")
                            myInitialElementLine = 0
                            return@scrollToLineWithShift true
                        }

                        return@scrollToLineWithShift false
                    }
                }
            }
        }
    }

    private fun PhpPsiElement.line(): Int {
        val document = PsiDocumentManager.getInstance(project).getDocument(containingFile)
        val lineNumber = if (document != null) {
            document.getLineNumber(textRange.startOffset) + 1
        } else {
            0
        }
        return lineNumber
    }

    private fun findParentClassOrFunction(element: PhpPsiElement): PhpPsiElement? {
        var elem = element as PsiElement
        var maxDepth = 30

        while (maxDepth > 0) {
            elem = elem.parent
            maxDepth--

            if (elem is FunctionImpl) {
                return elem
            }

            if (elem is PhpClassImpl) {
                return elem
            }

            if (elem is PhpFileImpl) {
                break
            }
        }

        return null
    }

    private fun PhpPsiElement.kphpFqn() = when (this) {
        is FunctionImpl -> convertFqnToKphpStyle(this.fqn)
        is PhpClassImpl -> convertFqnToKphpStyle(this.fqn)
        else -> ""
    }

    private fun convertFqnToKphpStyle(name: String): String {
        return "\\" + name.trimStart('\\')
            .replace(".", "::")
            .replace("\\", "\\\\")
    }
}
