package com.vk.admstorm.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.env.Env
import com.vk.admstorm.utils.extensions.normalizeSlashes
import java.io.File

object MyPathUtils {
    private var remoteRoot = ""

    private fun resolveProjectDir(project: Project): String? {
        return project.guessProjectDir()?.path ?: return null
    }

    fun foldUserHome(path: String): String {
        val userHome = System.getProperty("user.home")
        if (path.startsWith(userHome)) {
            return "~" + path.removePrefix(userHome)
        }
        return path
    }

    fun resolveRemoteRoot(project: Project): String? {
        if (remoteRoot.isNotEmpty()) {
            return remoteRoot
        }

        val res = CommandRunner.runRemotely(project, "echo ${Env.data.projectRoot}", 500)
        if (res.exitCode != 0) {
            return null
        }

        remoteRoot = res.stdout.trimEnd()
        return remoteRoot
    }

    fun relativeLocalPath(project: Project, path: String): String {
        val projectDir = resolveProjectDir(project) ?: return path.normalizeSlashes()
        return File(path).relativeTo(File(projectDir)).path.normalizeSlashes()
    }

    fun absoluteLocalPath(project: Project, path: String): String {
        if (File(path).isAbsolute) return path
        val projectDir = resolveProjectDir(project) ?: return path
        return "$projectDir/$path"
    }

    fun absoluteDataBasedRemotePath(project: Project, relativePath: String): String? {
        val remoteRoot = resolveRemoteRoot(project) ?: return null
        return "$remoteRoot/$relativePath"
    }

    fun absoluteLocalPathByRemotePath(project: Project, remotePath: String): String? {
        if (File(remotePath).isAbsolute) {
            return absoluteLocalPathByAbsoluteRemotePath(project, remotePath)
        }
        return absoluteLocalPathByRelativePhpFolderRemotePath(project, remotePath)
    }

    private fun absoluteLocalPathByRelativePhpFolderRemotePath(project: Project, remoteRelativePath: String): String? {
        val remoteRoot = resolveRemoteRoot(project) ?: return null
        val remoteAbsolutePath =
            if (remoteRelativePath.startsWith("${Env.data.phpSourceFolder}/"))
                "$remoteRoot/$remoteRelativePath"
            else
                "$remoteRoot/${Env.data.phpSourceFolder}/$remoteRelativePath"
        return absoluteLocalPathByAbsoluteRemotePath(project, remoteAbsolutePath)
    }

    fun absoluteLocalPathByAbsoluteRemotePath(project: Project, remotePath: String): String? {
        var remoteRoot = resolveRemoteRoot(project) ?: return null

        if (remotePath.startsWith(Env.data.kphpRelatedPathBegin)) {
            remoteRoot = remoteRoot.replace("/home/", "${Env.data.kphpRelatedPathBegin}/")
        }

        val remoteFile = File(remotePath)
        val remoteRootFile = File(remoteRoot)
        if (!remoteFile.isAbsolute) {
            return null
        }

        val relativeRemotePathFile = remoteFile.relativeTo(remoteRootFile)
        val projectDir = resolveProjectDir(project) ?: return null

        return "${projectDir.normalizeSlashes()}/$relativeRemotePathFile"
    }

    fun remotePathByLocalPath(project: Project, path: String): String {
        val rel = relativeLocalPath(project, path)
        return "$remoteRoot/$rel"
    }

    fun remotePhpFolderRelativePathByLocalPath(project: Project, path: String): String {
        if (Env.data.phpSourceFolder == "") return ""

        val rel = relativeLocalPath(project, path)
        return File(rel).relativeTo(File("${Env.data.phpSourceFolder}/")).path
    }
}
