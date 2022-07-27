package com.vk.admstorm.highlight.markers.admmarker

import com.vk.admstorm.env.Env
import com.vk.admstorm.ui.AdmIcons

class LoggerMarker : MarkerBrowser() {
    override fun getIcon() = AdmIcons.General.ExternalLinkArrow

    override fun getTooltip() = "Go to show log"

    override fun generateUrl(uri: String): String {
        val service = Env.data.services.getByKey("ch_logger") ?: return ""

        val prefix = "&section="

        return "${service.url}$prefix$uri"
    }
}
