package com.vk.admstorm.highlight.markers.admmarker

import com.vk.admstorm.env.Env
import com.vk.admstorm.ui.AdmIcons

class BufferMarker : MarkerBrowser() {
    override fun getIcon() = AdmIcons.General.ExternalLinkArrow

    override fun getTooltip() = "Go to buffer"

    override fun generateUrl(uri: String): String {
        val service = Env.data.services.getByKey("buffer_log") ?: return ""

        return "${service.url}?buffer_prefix=$uri"
    }
}
