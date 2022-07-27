package com.vk.admstorm.highlight.markers.admmarker

import com.vk.admstorm.env.Env
import com.vk.admstorm.ui.AdmIcons

class StatshouseMarker(private val mode: Mode) : MarkerBrowser() {
    override fun getIcon() = AdmIcons.General.ExternalLinkArrow

    override fun getTooltip() = "Go to statshouse"

    enum class Mode(val modeName: String) {
        VIEW("view"),
    }

    override fun generateUrl(uri: String): String {
        val service = Env.data.services.getByKey("statshouse") ?: return ""

        val baseUrl = service.url + "/" + mode.modeName
        val prefix = "?s="

        return "$baseUrl$prefix$uri"
    }
}
