package com.vk.admstorm.highlight.markers.admmarker

interface IMarkerBrowser : IMarker {
    fun generateUrl(uri: String): String
}
