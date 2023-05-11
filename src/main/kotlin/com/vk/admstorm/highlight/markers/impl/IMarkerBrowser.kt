package com.vk.admstorm.highlight.markers.impl

interface IMarkerBrowser : IMarker {
    fun generateUrl(uri: String): String
}
