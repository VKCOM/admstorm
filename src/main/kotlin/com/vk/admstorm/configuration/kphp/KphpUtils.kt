package com.vk.admstorm.configuration.kphp

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.vk.admstorm.env.Env
import com.vk.admstorm.utils.MyPathUtils
import java.util.stream.Collectors

object KphpUtils {
    private val LOG = logger<KphpUtils>()
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
        }.collect(Collectors.toList())
    }

    fun scriptBinaryPath(project: Project): String {
        val userName = MyPathUtils.remoteUserName()

        return if (userName != "") {
            "${Env.data.kphpRelatedPathBegin}/$userName/${Env.data.kphpScriptBinaryPath}"
        } else {
            LOG.warn("User name on server is empty")

            val remoteRoot = MyPathUtils.resolveRemoteRoot(project) ?: ""
            "$remoteRoot/${Env.data.kphpScriptBinaryPath}"
        }
    }
}
