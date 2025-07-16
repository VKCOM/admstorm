package com.vk.admstorm.ssh

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.vk.admstorm.settings.AdmStormSettingsState

@Service(Service.Level.APP)
class OpenSCProviderManager {
    companion object {
        fun getInstance() = service<OpenSCProviderManager>()
    }

    private var openSCCachedPath: String? = null

    private fun findOpenSCPath(): String? {
        val customOpenSCPath = AdmStormSettingsState.getInstance().customOpenSCPath.trim()
        if (customOpenSCPath.isNotBlank()) {
            return customOpenSCPath
        }

        return OpenSCProviderDetector.detectPath()
    }

    fun getOpenSCPath(): String? {
        if (openSCCachedPath != null) {
            return openSCCachedPath
        }

        openSCCachedPath = findOpenSCPath()
        return openSCCachedPath
    }

    fun dropCache() {
        openSCCachedPath = null
    }
}
