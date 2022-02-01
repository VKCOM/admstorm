package com.vk.admstorm.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.vk.admstorm.env.Env
import kotlin.streams.toList

object MyKphpUtils {
    private val LOG = Logger.getInstance(MyKphpUtils::class.java)
    private var defaultIncludeDirsFlag = ""

    fun includeDirsAsFlags(project: Project): String {
        if (defaultIncludeDirsFlag.isNotEmpty()) {
            return defaultIncludeDirsFlag
        }

        defaultIncludeDirsFlag = Env.data.kphpRelativeIncludeDirs.stream().map {
            if (it.startsWith('/')) "-I $it"
            else "-I " + MyPathUtils.absoluteDataBasedRemotePath(project, it)
        }.toArray().joinToString(" ")

        return defaultIncludeDirsFlag
    }

    fun includeDirsAsList(project: Project): List<String> {
        return Env.data.kphpRelativeIncludeDirs.stream().map {
            if (it.startsWith('/')) it
            else MyPathUtils.absoluteDataBasedRemotePath(project, it) ?: it
        }.toList()
    }

    fun scriptBinaryPath(project: Project): String {
        val remoteRoot = MyPathUtils.resolveRemoteRoot(project) ?: ""
        try {
            val userName = remoteRoot.split("/")[2]
            return "${Env.data.kphpRelatedPathBegin}/$userName/${Env.data.kphpScriptBinaryPath}"
        } catch (e: Exception) {
            LOG.warn("Unexpected exception while scriptBinaryPath", e)
        }

        return "$remoteRoot/${Env.data.kphpScriptBinaryPath}"
    }
}
