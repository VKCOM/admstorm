package com.vk.admstorm.highlight.markers.admmarker

import com.vk.admstorm.env.Env
import com.vk.admstorm.env.Env.getByKey
import com.vk.admstorm.highlight.markers.impl.IMarkerBrowser
import com.vk.admstorm.ui.AdmIcons

class ABMarker : IMarkerBrowser {
    override val icon = AdmIcons.General.ExternalLinkArrow

    override val tooltip = "Go to A/B Platform"

    override fun generateUrl(uri: String): String {
        val service = Env.data.services.getByKey("ab_platform") ?: return ""

        return "${service.url}?$uri"
    }
}
