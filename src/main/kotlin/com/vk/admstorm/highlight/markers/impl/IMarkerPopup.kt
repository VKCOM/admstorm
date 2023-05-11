package com.vk.admstorm.highlight.markers.impl

import javax.swing.JComponent

interface IMarkerPopup<TModel> : IMarker {
    fun generatePopup(model: TModel): JComponent
}
