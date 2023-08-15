package com.vk.admstorm

import com.intellij.json.psi.JsonStringLiteral
import com.intellij.json.psi.impl.JsonRecursiveElementVisitor
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MyUtils
import com.vk.admstorm.utils.extensions.unquote

@Service(Service.Level.PROJECT)
class AdmService(private var myProject: Project) {
    companion object {
        private val LOG = logger<AdmService>()
        val PLUGIN_ID = PluginId.getId("com.vk.admstorm")

        fun getInstance(project: Project) = project.service<AdmService>()
    }

    /**
     * Calculated in [needBeEnabled] and cached in this field.
     *
     * null value means that the value has not yet been calculated.
     */
    private var myNeedBeEnabled: Boolean? = null

    /**
     * Checks that the value has been calculated.
     *
     * Use [Project.pluginEnabled()].
     */
    fun isInitialized(): Boolean {
        return myNeedBeEnabled != null
    }

    /**
     * Checks if the current project is vkcom to determine
     * if plugin functionality needs to be enabled.
     *
     * Use [Project.pluginEnabled()].
     */
    fun needBeEnabled(): Boolean {
        if (myNeedBeEnabled != null) {
            return myNeedBeEnabled!!
        }

        val composerFile = MyUtils.virtualFileByName(MyPathUtils.absoluteLocalPath(myProject, "composer.json"))
        if (composerFile == null) {
            LOG.info("Checking the current project for enable plugin: Composer file not found")
            myNeedBeEnabled = false
            return false
        }

        val composerPsiFile = PsiManager.getInstance(myProject).findFile(composerFile)
        if (composerPsiFile == null) {
            LOG.info("Checking the current project for enable plugin: Composer file found, but psi file for it not found")
            myNeedBeEnabled = false
            return false
        }

        var containsGlobal = false
        composerPsiFile.accept(object : JsonRecursiveElementVisitor() {
            override fun visitStringLiteral(lit: JsonStringLiteral) {
                containsGlobal = containsGlobal || lit.text.unquote() == "vk/global"
            }
        })

        myNeedBeEnabled = containsGlobal

        LOG.info("Checking the current project for enable plugin: Is vkcom: $myNeedBeEnabled")
        return myNeedBeEnabled!!
    }
}
