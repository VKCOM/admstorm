package com.vk.admstorm.highlight.markers.admmarker

import javax.swing.Icon

interface IMarker {
    fun getIcon(): Icon

    fun getTooltip(): String
}
