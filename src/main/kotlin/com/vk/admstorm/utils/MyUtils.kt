package com.vk.admstorm.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.env.Env
import org.jdom.Element
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.security.MessageDigest
import kotlin.system.measureTimeMillis

object MyUtils {
    val LOG = Logger.getInstance(MyUtils::class.java)

    fun copyToClipboard(data: String) {
        Toolkit.getDefaultToolkit().systemClipboard
            .setContents(
                StringSelection(
                    data
                ), null
            )
    }

    fun createHaste(project: Project, data: String): String {
        val output = data.replace("\"", "\\\"").replace("$", "\\$")
        return CommandRunner.runRemotely(project, "echo \"$output\" | ${Env.data.pasteBinCommand}").stdout
    }

    fun virtualFileByRelativePath(project: Project, filepath: String): VirtualFile? {
        val projectDir = project.guessProjectDir()?.path ?: ""
        val absolutePath = "${projectDir}/${filepath}"
        return LocalFileSystem.getInstance().findFileByIoFile(File(absolutePath))
    }

    fun virtualFileByName(name: String): VirtualFile? {
        return LocalFileSystem.getInstance().findFileByIoFile(File(name))
    }

    private fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }

    fun md5file(file: VirtualFile?): String {
        if (file == null) {
            LOG.info("Passes null file to md5file()")
            return ""
        }

        val ioFile = File(file.path)
        if (!ioFile.exists()) {
            LOG.info("File ${file.path} doesn't exist")
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
    fun measureTime(log: Logger, subject: String, block: () -> Unit) {
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
    fun <T> measureTimeValue(log: Logger, subject: String, block: () -> T): T {
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
}

fun String.indentWidth(): Int = indexOfFirst { !it.isWhitespace() }.let { if (it == -1) length else it }

fun String.fixIndent(): String {
    val lines = lines()

    val firstLine = lines[0]
    val firstLineIndent = firstLine.indentWidth()

    val minCommonIndent = lines
        .slice(1 until lines.size)
        .filter { it.isNotBlank() }
        .minOfOrNull { it.indentWidth() } ?: 0

    if (firstLineIndent < minCommonIndent) {
        val diff = minCommonIndent - firstLineIndent
        return " ".repeat(diff) + this
    }

    return this
}

fun Element.writeString(name: String, value: String) {
    val opt = Element("option")
    opt.setAttribute("name", name)
    opt.setAttribute("value", value)
    addContent(opt)
}

fun Element.readString(name: String): String? =
    children
        .find { it.name == "option" && it.getAttributeValue("name") == name }
        ?.getAttributeValue("value")

fun Element.writeBool(name: String, value: Boolean) {
    writeString(name, value.toString())
}

fun Element.readBool(name: String): Boolean? =
    readString(name)?.toBoolean()
