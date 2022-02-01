package com.vk.admstorm.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.vk.admstorm.notifications.AdmErrorNotification
import java.io.File
import java.util.function.BiConsumer
import javax.swing.SwingConstants

object MyEditorUtils {
    /**
     * Splits the window vertically into two and opens the passed file in the right.
     *
     * @param virtualFile file to be opened in the right editor
     * @return EditorWindow that represents the right editor or null if an error occurs
     */
    private fun splitCurrentVertically(project: Project, virtualFile: VirtualFile): EditorWindow? {
        val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
        val curWindow = fileEditorManager.currentWindow
        return curWindow.split(SwingConstants.VERTICAL, false, virtualFile, true)
    }

    /**
     * Applies the passed function for each editor within the passed
     * window that is an editor of the specified template type [T].
     *
     * For example:
     *  [MyEditorUtil.applyEachEditor<PsiAwareTextEditorImpl>(editorWindow) {}]
     *
     * Applies a function to each text editor in the passed window.
     */
    private inline fun <reified T> applyEachEditor(editorWindow: EditorWindow, cb: (T) -> Unit) {
        editorWindow.editors.forEach { editorComposite ->
            editorComposite.editors.forEach { editor ->
                if (editor is T) {
                    cb(editor)
                }
            }
        }
    }

    /**
     * Applies the passed function for last open editor within the passed
     * window that is an editor of the specified template type [T].
     *
     * For example:
     *  [MyEditorUtil.applyLastEditor<PsiAwareTextEditorImpl>(editorWindow) {}]
     *
     * Applies a function to last open text editor in the passed window.
     */
    inline fun <reified T> applyLastEditor(editorWindow: EditorWindow, cb: (T) -> Unit) {
        val editors = editorWindow.editors
        val lastEditor = if (editors.isNotEmpty()) editors.last() else return

        lastEditor.editors.forEach { editor ->
            if (editor is T) {
                cb(editor)
            }
        }
    }

    /**
     * Scrolls the passed editor to the line that first matches the passed callback.
     */
    private fun scrollToLine(fileEditor: PsiAwareTextEditorImpl, isNeedLine: (String) -> Boolean) {
        scrollToLineWithShift(fileEditor, 0, isNeedLine)
    }

    /**
     * Scrolls the passed editor to the line that first matches the passed callback + shift.
     */
    fun scrollToLineWithShift(fileEditor: PsiAwareTextEditorImpl, shift: Int, isNeedLine: (String) -> Boolean) {
        val editor = fileEditor.editor
        val text = LoadTextUtil.loadText(fileEditor.file)

        var needLineIndex = 0

        text.split("\n").forEachIndexed { index, line ->
            if (needLineIndex == 0 && isNeedLine(line)) {
                needLineIndex = index
                return@forEachIndexed
            }
        }

        editor.caretModel.moveToLogicalPosition(
            LogicalPosition(needLineIndex + shift, 0)
        )

        val scrollingModel = editor.scrollingModel
        scrollingModel.disableAnimation()
        scrollingModel.scrollToCaret(ScrollType.CENTER_UP)
        scrollingModel.enableAnimation()
    }

    /**
     * Opens the passed file in a separate window to the right of the current one.
     * And then calls the passed [onReady] callback function.
     *
     * @param file File to open
     */
    fun openFileInRightTab(project: Project, file: File, onReady: BiConsumer<EditorWindow, VirtualFile>?) {
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        if (virtualFile == null) {
            AdmErrorNotification("${file.absolutePath} does not exists!").show()
            return
        }

        NonProjectFileWritingAccessProvider.allowWriting(listOf(virtualFile))
        ApplicationManager.getApplication().invokeLater {
            val rightWindow = splitCurrentVertically(project, virtualFile) ?: return@invokeLater

            applyEachEditor<PsiAwareTextEditorImpl>(rightWindow) { fileEditor ->
                fileEditor.editor.settings.isUseSoftWraps = true

                scrollToLine(fileEditor) { line ->
                    line.contains(" f$")
                }
            }

            onReady?.accept(rightWindow, virtualFile)
        }
    }

    /**
     * Opens the passed file with [filepath] in a separate window on the given [line].
     */
    fun openFileOnLine(project: Project, filepath: String, line: Int) {
        val file = WriteAction
            .compute<VirtualFile?, RuntimeException> {
                LocalFileSystem.getInstance().refreshAndFindFileByPath(
                    FileUtil.toSystemIndependentName(filepath)
                )
            }

        if (file == null) {
            AdmErrorNotification("$filepath does not exists!").show()
            return
        }

        file.refresh(false, false)

        ApplicationManager.getApplication().runReadAction {
            val editor = FileEditorManager.getInstance(project)
                .openTextEditor(OpenFileDescriptor(project, file), true)
            editor!!.caretModel.moveToLogicalPosition(LogicalPosition(line - 1, 0))
            editor.scrollingModel.disableAnimation()
            editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
            editor.scrollingModel.enableAnimation()
        }
    }
}
