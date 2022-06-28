package com.vk.admstorm.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.env.Env
import com.vk.admstorm.utils.extensions.toHex
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL
import java.security.MessageDigest
import java.util.*
import kotlin.system.measureTimeMillis

object MyUtils {
    val LOG = logger<MyUtils>()

    fun copyToClipboard(data: String) {
        Toolkit.getDefaultToolkit().systemClipboard
            .setContents(
                StringSelection(
                    data
                ), null
            )
    }

    fun getClipboardContents(): String? {
        val contents = try {
            Toolkit.getDefaultToolkit().systemClipboard.getContents(null)
        } catch (e: IllegalStateException) {
            LOG.warn("getClipboardContents exception", e)
            null
        } ?: return null

        val data = try {
            contents.getTransferData(DataFlavor.stringFlavor)
        } catch (e: Exception) {
            LOG.warn("getClipboardContents exception", e)
            null
        } ?: return null

        return data as? String
    }

    fun readFromWeb(path: String): String? {
        val url = try {
            URL(path)
        } catch (e: Exception) {
            LOG.warn("readFromWeb exception", e)
            return null
        }
        val inputStream = try {
            url.openStream()
        } catch (e: Exception) {
            LOG.warn("readFromWeb exception", e)
            return null
        }
        val result = StringBuilder()
        try {
            BufferedReader(InputStreamReader(inputStream)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    result.append(line + "\n")
                }
            }
        } catch (e: MalformedURLException) {
            LOG.warn("readFromWeb exception", e)
            return null
        } catch (e: IOException) {
            LOG.warn("readFromWeb exception", e)
            return null
        }

        return result.toString().removeSuffix("\n")
    }

    fun createHaste(project: Project, data: String): String {
        val output = data.replace("\"", "\\\"").replace("$", "\\$")
        return CommandRunner.runRemotely(project, "echo \"$output\" | ${Env.data.pasteBinCommand}").stdout
    }

    fun virtualFileByRelativePath(project: Project, filepath: String): VirtualFile? {
        val projectDir = project.guessProjectDir()?.path ?: ""
        val absolutePath = "$projectDir/$filepath"
        return LocalFileSystem.getInstance().findFileByIoFile(File(absolutePath))
    }

    fun virtualFileByName(name: String): VirtualFile? {
        return LocalFileSystem.getInstance().findFileByIoFile(File(name))
    }

    fun psiFileByName(project: Project, name: String): PsiFile? {
        return virtualFileByName(name)?.let { PsiManager.getInstance(project).findFile(it) }
    }

    fun md5file(file: VirtualFile?): String {
        if (file == null) {
            LOG.warn("Passes null file to md5file()")
            return ""
        }

        val ioFile = File(file.path)
        if (!ioFile.exists()) {
            LOG.warn("File ${file.path} doesn't exist")
            return ""
        }

        val content = ioFile.readBytes()
        val md5 = MessageDigest.getInstance("MD5").digest(content).toHex()

        LOG.info("MD5 for ${file.path}: $md5")

        return md5
    }

    /**
     * Measures the execution time of the [block] in milliseconds and
     * writes it to the logs via [log] with the passed [subject].
     */
    inline fun measureTime(log: Logger, subject: String, block: () -> Unit) {
        val elapsed = measureTimeMillis {
            block()
        }
        log.info("Elapsed time ('$subject'): ${elapsed}ms")
    }

    /**
     * Measures the execution time of the [block] in milliseconds,
     * writes it to the logs via [log] with the passed [subject]
     * and returns the result of the [block] execution.
     */
    inline fun <T> measureTimeValue(log: Logger, subject: String, block: () -> T): T {
        var res: T
        val elapsed = measureTimeMillis {
            res = block()
        }
        log.info("Elapsed time ('$subject'): ${elapsed}ms")
        return res
    }

    inline fun runBackground(
        project: Project,
        @NlsContexts.ProgressTitle title: String,
        crossinline task: (indicator: ProgressIndicator) -> Unit
    ) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, title, true) {
            override fun run(indicator: ProgressIndicator) {
                LOG.info("Start Backgroundable task ('$title')")
                indicator.isIndeterminate = true
                task(indicator)
                LOG.info("End Backgroundable task ('$title')")
            }
        })
    }

    inline fun runConditionalModal(
        project: Project,
        @NlsContexts.ProgressTitle title: String,
        crossinline task: (indicator: ProgressIndicator) -> Unit
    ) {
        ProgressManager.getInstance().run(object : Task.ConditionalModal(
            project, title, true, PerformInBackgroundOption.DEAF,
        ) {
            override fun run(indicator: ProgressIndicator) {
                LOG.info("Start ConditionalModal task ('$title')")
                indicator.isIndeterminate = true
                task(indicator)
                LOG.info("End ConditionalModal task ('$title')")
            }
        })
    }

    inline fun <T> invokeAndWaitResult(crossinline block: () -> T): T {
        var result: T? = null

        ApplicationManager.getApplication().invokeAndWait {
            result = block()
        }

        return result!!
    }

    inline fun executeOnPooledThread(crossinline block: () -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread { block() }
    }

    inline fun invokeAfter(delay: Long, crossinline block: () -> Unit) {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                block()
            }
        }, delay)
    }
}
