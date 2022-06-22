package com.vk.admstorm.configuration.phpunit

import com.intellij.json.JsonUtil
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.project.Project
import com.vk.admstorm.utils.MyUtils
import com.vk.admstorm.utils.MyUtils.unquote
import java.io.File

object PackagesTestUtils {
    fun rootFolder(startPath: String): String? {
        val start = File(startPath)

        var count = 0
        var curFolder = start
        while (curFolder.path.contains("/packages/")) {
            if (count > 5) {
                break
            }

            val curComposerJson = File(curFolder, "composer.json")
            if (curComposerJson.exists()) {
                return curComposerJson.parent
            }

            curFolder = curFolder.parentFile
            count++
        }

        return null
    }

    fun packageName(project: Project, root: String?): String? {
        if (root == null) {
            return null
        }

        val composerFile = File(root, "composer.json")
        if (!composerFile.exists()) {
            return null
        }

        val psiFile = MyUtils.psiFileByName(project, composerFile.path) as? JsonFile ?: return null
        val topLevelObject = JsonUtil.getTopLevelObject(psiFile) ?: return null

        return topLevelObject.findProperty("name")?.value?.text?.unquote()
    }
}
