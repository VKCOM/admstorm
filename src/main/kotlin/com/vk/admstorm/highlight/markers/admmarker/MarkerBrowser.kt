package com.vk.admstorm.highlight.markers.admmarker

import com.vk.admstorm.env.Service

abstract class MarkerBrowser : IMarkerBrowser {
    fun List<Service>.getByKey(key: String) = firstOrNull { it.key == key }
}
