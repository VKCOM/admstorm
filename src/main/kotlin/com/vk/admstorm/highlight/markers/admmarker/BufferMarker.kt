package com.vk.admstorm.highlight.markers.admmarker

import com.vk.admstorm.env.Env
import com.vk.admstorm.env.Env.getByKey
import com.vk.admstorm.highlight.markers.impl.IMarkerBrowser
import com.vk.admstorm.ui.AdmIcons

class BufferMarker : IMarkerBrowser {
    override val icon = AdmIcons.General.ExternalLinkArrow

    override val tooltip = "Go to buffer"

    override fun generateUrl(uri: String): String {
        val service = Env.data.services.getByKey("buffer_log") ?: return ""

        return "${service.url}?buffer_prefix=$uri"
    }
}
