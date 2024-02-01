package com.vk.admstorm.playground

import com.intellij.dvcs.ui.CompareBranchesDialog
import com.intellij.execution.OutputListener
import com.intellij.execution.process.ProcessEvent
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ToolbarLabelAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.WindowWrapper
import com.intellij.openapi.ui.WindowWrapperBuilder
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.remote.ColoredRemoteProcessHandler
import com.intellij.ssh.process.SshExecProcess
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.JBTabsPaneImpl
import com.intellij.ui.OnePixelSplitter
import com.intellij.util.ui.JBUI
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.PhpPsiElementFactory
import com.jetbrains.php.lang.psi.elements.ClassReference
import com.jetbrains.php.lang.psi.elements.PhpUse
import com.vk.admstorm.actions.ActionToolbarFastEnableAction
import com.vk.admstorm.configuration.kphp.KphpScriptRunner
import com.vk.admstorm.configuration.kphp.KphpUtils.scriptBinaryPath
import com.vk.admstorm.console.Console
import com.vk.admstorm.notifications.AdmErrorNotification
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.psi.PhpRecursiveElementVisitor
import com.vk.admstorm.services.HastebinService
import com.vk.admstorm.transfer.TransferService
import com.vk.admstorm.utils.MyPathUtils.remotePathByLocalPath
import com.vk.admstorm.utils.MySshUtils
import com.vk.admstorm.utils.MyUtils
import com.vk.admstorm.utils.MyUtils.executeOnPooledThread
import com.vk.admstorm.utils.MyUtils.invokeAfter
import com.vk.admstorm.utils.extensions.fixIndent
import com.vk.admstorm.utils.extensions.pluginEnabled
import java.io.File
import java.io.IOException
import javax.swing.JLabel
import javax.swing.SwingConstants

class KphpPlaygroundWindow(private val myProject: Project) : Disposable {
    companion object {
        private val LOG = logger<KphpPlaygroundWindow>()

        private const val GENERATED_FILE_HEADER = """<?php

#ifndef KPHP
require_once 'www/autoload.php';
require_once 'vendor/autoload.php';
#endif

"""
    }

    internal enum class State {
        Uploading,
        Compilation,
        Run,
        End
    }

    private var myState: State = State.End

    private val myWrapper: WindowWrapper
    private lateinit var myEditor: EditorEx

    private val myMainComponent = JBUI.Panels.simplePanel()

    private val myActionToolbar: ActionToolbar
    private val myActionGroup = DefaultActionGroup()
    private val myShareLabel =
        JLabel("Link copied", AllIcons.General.InspectionsOK, SwingConstants.LEFT).apply { isVisible = false }
    private val myLoaderLabel = JLabel("Uploading...", AnimatedIcon.Default(), SwingConstants.LEFT)
    private val myHeaderComponent = JBUI.Panels.simplePanel()

    private val myTabs = JBTabsPaneImpl(myProject, SwingConstants.TOP, this)
    private val myEditorOutputSplitter = OnePixelSplitter(true, 0.6f)
    private val myDiffViewer = KphpPhpDiffViewer(myProject)

    private val myKphpCompilationProcessConsole = Console(myProject, withFilters = false)

    private val myScriptFile: VirtualFile

    private lateinit var myProcessHandler: ColoredRemoteProcessHandler<SshExecProcess>

    private val myRunCompilationAction = object : ActionToolbarFastEnableAction(
        "Run", AllIcons.Actions.Execute,
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            runCompilationAction()
        }
    }

    private val myStopCompilationAction = object : ActionToolbarFastEnableAction(
        "Stop", AllIcons.Actions.Suspend,
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            myProcessHandler.destroyProcess()
            setEnabled(false)
        }
    }

    private val myShareAction = object : ActionToolbarFastEnableAction(
        "Share Hastebin link to code", AllIcons.CodeWithMe.CwmShared,
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            val content = myEditor.document.text

            executeOnPooledThread {
                val link = HastebinService.getInstance(myProject).createHaste(content)
                if (link == null) {
                    AdmNotification()
                        .withTitle("Hastebin service unavailable. Try again later")
                        .show()
                    return@executeOnPooledThread
                }

                MyUtils.copyToClipboard(link)
                invokeLater {
                    myShareLabel.text = "Link copied"
                    myShareLabel.isVisible = true
                    invokeAfter(2000) {
                        myShareLabel.isVisible = false
                    }
                }
            }
        }
    }

    private val mySharePasteAction = object : ActionToolbarFastEnableAction(
        "Paste code from Hastebin link", AllIcons.Actions.MenuPaste,
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            val link = MyUtils.getClipboardContents() ?: return
            if (!link.contains("pastebin")) {
                showShareLabel("Not a Hastebin link", isError = true)
                return
            }
            val rawLink = link.replace(".com/", ".com/raw/")

            executeOnPooledThread {
                val data = MyUtils.readFromWeb(rawLink)
                if (data == null) {
                    showShareLabel("Unable get code from link", isError = true)
                    return@executeOnPooledThread
                }
                invokeLater {
                    runWriteAction {
                        myEditor.document.setText(data)
                    }

                    showShareLabel("Code pasted")
                }
            }
        }

        private fun showShareLabel(text: String, isError: Boolean = false) {
            if (isError) {
                myShareLabel.icon = AllIcons.Ide.FatalError
            }
            myShareLabel.text = text
            myShareLabel.isVisible = true
            invokeAfter(2000) {
                myShareLabel.isVisible = false
                myShareLabel.icon = AllIcons.General.InspectionsOK
            }
        }
    }

    class LabelAction(private val text: String) : ToolbarLabelAction() {
        override fun update(e: AnActionEvent) {
            super.update(e)
            val project = e.project

            if (project == null || !project.pluginEnabled()) {
                e.presentation.isEnabledAndVisible = false
                return
            }

            e.presentation.text = "$text:"
        }
    }

    init {
        val tmpDir = File(System.getProperty("java.io.tmpdir"))
        val tempFile = File(tmpDir, "kphp_script_dummy.php")

        try {
            tempFile.createNewFile()
        } catch (e: IOException) {
            LOG.warn("Unexpected exception while create temp file '${tempFile.path}'", e)
        }

        val tmpVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(tempFile.absolutePath)
        if (tmpVirtualFile == null) {
            LOG.warn("kphp_script_dummy.php not found")
            AdmErrorNotification("Try restarting PHPStorm and try again")
                .withTitle("Problem with temp file for KPHP Playground")
                .show()

            throw IllegalStateException("kphp_script_dummy.php not found")
        }

        val document = FileDocumentManager.getInstance().getDocument(tmpVirtualFile)!!

        myScriptFile = tmpVirtualFile
        myEditor = EditorFactory.getInstance().createEditor(document, myProject, PhpFileType.INSTANCE, false)
                as EditorEx

        val documentIsEmpty = document.textLength == 0
        if (documentIsEmpty) {
            WriteCommandAction.runWriteCommandAction(myProject) {
                document.setText(GENERATED_FILE_HEADER)
                myEditor.caretModel.moveToOffset(100)
                PsiDocumentManager.getInstance(myProject).doPostponedOperationsAndUnblockDocument(document)
            }
        }

        myEditorOutputSplitter.firstComponent = myEditor.component

        myTabs.insertTab("KPHP Compilation", null, myKphpCompilationProcessConsole.component(), "", 0)
        myTabs.insertTab("Output", null, myDiffViewer.component, "", 1)

        myEditorOutputSplitter.secondComponent = myTabs.component

        myStopCompilationAction.setEnabled(false)

        myActionGroup.add(LabelAction("Run"))
        myActionGroup.add(myRunCompilationAction)
        myActionGroup.add(myStopCompilationAction)
        myActionGroup.add(LabelAction("Share"))
        myActionGroup.add(myShareAction)
        myActionGroup.add(mySharePasteAction)

        myActionToolbar = ActionManager.getInstance()
            .createActionToolbar("KphpPlaygroundToolBar", myActionGroup, true)
        myActionToolbar.targetComponent = myEditorOutputSplitter

        myActionToolbar.updateActionsImmediately()

        myHeaderComponent
            .addToLeft(myActionToolbar.component)
            .addToCenter(myShareLabel)
            .addToRight(myLoaderLabel)
            .apply { border = JBUI.Borders.empty(0, 5) }

        myMainComponent
            .addToTop(myHeaderComponent)
            .addToCenter(myEditorOutputSplitter)

        myWrapper = WindowWrapperBuilder(WindowWrapper.Mode.FRAME, myMainComponent)
            .setProject(myProject)
            .setTitle("KPHP Playground")
            .setPreferredFocusedComponent(myEditor.component)
            .setDimensionServiceKey(CompareBranchesDialog::class.java.name)
            .setOnCloseHandler {
                dispose()
                true
            }
            .build()

        setState(State.End)
    }

    private fun runCompilationAction() {
        ApplicationManager.getApplication().invokeAndWait {
            myKphpCompilationProcessConsole.clear()
            myDiffViewer.clear()
            myTabs.selectedIndex = 0
        }

        compileScriptBinary()
    }

    private fun saveFile() {
        ApplicationManager.getApplication().invokeAndWait {
            ApplicationManager.getApplication().runWriteAction {
                val document = FileDocumentManager.getInstance().getDocument(myScriptFile) ?: return@runWriteAction
                FileDocumentManager.getInstance().saveDocumentAsIs(document)
            }
        }
    }

    private fun setState(s: State) {
        myState = s
        when (s) {
            State.Uploading -> {
                myLoaderLabel.isVisible = true
                myLoaderLabel.text = "Uploading..."

                myRunCompilationAction.setEnabled(false)
                myStopCompilationAction.setEnabled(true)
            }
            State.Compilation -> {
                myLoaderLabel.isVisible = true
                myLoaderLabel.text = "Compilation..."
            }
            State.Run -> {
                myLoaderLabel.isVisible = true
                myLoaderLabel.text = "Running..."
            }
            State.End -> {
                myLoaderLabel.isVisible = false

                myRunCompilationAction.setEnabled(true)
                myStopCompilationAction.setEnabled(false)
            }
        }

        myMainComponent.revalidate()
        myMainComponent.repaint()

        invokeLater {
            myActionToolbar.updateActionsImmediately()
        }
    }

    private fun scriptPath(project: Project): String {
        val projectDir = project.guessProjectDir()?.path ?: ""
        return "$projectDir/../kphp_script_dummy.php"
    }

    private fun compileScriptBinary() {
        saveFile()
        setState(State.Uploading)

        TransferService.getInstance(myProject).uploadFile(myScriptFile, scriptPath(myProject)) {
            setState(State.Compilation)

            val command = KphpScriptRunner.buildCommand(myProject, "../../kphp_script_dummy.php")
            myProcessHandler = MySshUtils.exec(myProject, command, workingDir = "~/") ?: return@uploadFile

            myKphpCompilationProcessConsole.clear()
            myKphpCompilationProcessConsole.view().attachToProcess(myProcessHandler)

            val outputListener = object : OutputListener() {
                override fun processTerminated(event: ProcessEvent) {
                    super.processTerminated(event)

                    if (event.exitCode != 0) {
                        setState(State.End)
                        return
                    }

                    setState(State.Run)

                    val phpOutput = executePhpScript()
                    val kphpOutput = executeKphpScriptBinary()

                    myDiffViewer.withPhpOutput(phpOutput)
                    myDiffViewer.withKphpOutput(kphpOutput)

                    invokeLater {
                        myTabs.selectedIndex = 1
                    }

                    setState(State.End)
                }
            }

            myProcessHandler.addProcessListener(outputListener)
            myProcessHandler.startNotify()
        }
    }

    private fun executePhpScript(): String {
        val remoteScriptName = remotePathByLocalPath(myProject, scriptPath(myProject))
        val command = "php $remoteScriptName"
        val handler = MySshUtils.exec(
            myProject, command,
            "php"
        ) ?: return ""

        val outputListener = OutputListener()
        handler.addProcessListener(outputListener)

        handler.startNotify()
        handler.waitFor()

        return outputListener.output.stdout.ifEmpty { "<no output>" }
    }

    private fun executeKphpScriptBinary(): String {
        val command = "${scriptBinaryPath(myProject)} --Xkphp-options --disable-sql"
        val handler = MySshUtils.exec(
            myProject, command,
            "kphp"
        ) ?: return ""

        val outputListener = OutputListener()
        handler.addProcessListener(outputListener)

        handler.startNotify()
        handler.waitFor()

        return outputListener.output.stdout.ifEmpty { "<no output>" }
    }

    fun show() {
        myWrapper.show()
    }

    fun withCode(file: PsiFile, importClasses: Boolean, code: String) {
        val template = GENERATED_FILE_HEADER + processCode(file, importClasses, code) + "\n\n"

        val document = ApplicationManager.getApplication().runReadAction(Computable {
            FileDocumentManager.getInstance().getDocument(myScriptFile)
        })

        ApplicationManager.getApplication().invokeAndWait {
            ApplicationManager.getApplication().runWriteAction {
                if (document != null) {
                    document.setText(template)
                    FileDocumentManager.getInstance().saveDocumentAsIs(document)
                }
            }
        }

        myTabs.selectedIndex = 0
        myWrapper.show()

        invokeLater {
            runCompilationAction()
        }
    }

    private fun processCode(originalFile: PsiFile, importClasses: Boolean, code: String): String {
        val useStatements = if (importClasses) generateUseStatements(originalFile, code) else ""
        return useStatements + code.fixIndent().trimIndent()
    }

    private fun generateUseStatements(originalFile: PsiFile, code: String): String {
        val file = PhpPsiElementFactory.createPsiFileFromText(myProject, code)

        val uses = PsiTreeUtil.findChildrenOfType(originalFile, PhpUse::class.java)
        val neededUses = mutableListOf<PhpUse>()

        file.accept(object : PhpRecursiveElementVisitor() {
            override fun visitPhpClassReference(classReference: ClassReference?) {
                val use = uses.find {
                    it.targetReference?.name == classReference?.name || it.aliasName == classReference?.name
                } ?: return

                val alreadyAdded = neededUses.find { it.parent.text == use.parent.text } != null
                if (!alreadyAdded) {
                    neededUses.add(use)
                }
            }
        })

        if (neededUses.isNotEmpty()) {
            return neededUses.joinToString("\n") { it.parent.text } + "\n\n"
        }

        return ""
    }

    override fun dispose() {
        if (!myEditor.isDisposed) {
            EditorFactory.getInstance().releaseEditor(myEditor)
        }

        if (!myDiffViewer.isDisposed) {
            myDiffViewer.dispose()
        }
    }
}
