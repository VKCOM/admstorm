package com.vk.admstorm.highlight.markers.admmarker

import javax.swing.JComponent

interface IMarkerPopup<TModel> : IMarker {
    fun generatePopup(model: TModel): JComponent
}
