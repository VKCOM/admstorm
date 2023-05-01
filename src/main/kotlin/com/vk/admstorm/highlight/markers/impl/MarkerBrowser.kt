package com.vk.admstorm.highlight.markers.impl

import com.vk.admstorm.env.Service

abstract class MarkerBrowser : IMarkerBrowser {
    fun List<Service>.getByKey(key: String) = firstOrNull { it.key == key }
}
