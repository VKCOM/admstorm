package com.vk.admstorm.highlight.markers.admmarker

import com.vk.admstorm.env.Env
import com.vk.admstorm.env.getByKey
import com.vk.admstorm.highlight.markers.impl.IMarkerBrowser
import com.vk.admstorm.ui.AdmIcons

class LoggerMarker : IMarkerBrowser {
    override val icon = AdmIcons.General.ExternalLinkArrow

    override val tooltip = "Go to show log"

    override fun generateUrl(uri: String): String {
        val service = Env.data.services.getByKey("ch_logger") ?: return ""

        val prefix = "&section="

        return "${service.url}$prefix$uri"
    }
}
