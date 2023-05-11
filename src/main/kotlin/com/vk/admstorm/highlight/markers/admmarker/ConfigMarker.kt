package com.vk.admstorm.highlight.markers.admmarker

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.vk.admstorm.admscript.utils.DataResponse
import com.vk.admstorm.highlight.markers.impl.MarkerService
import com.vk.admstorm.utils.UiDslBuilderUtils.monospace
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.swing.JComponent

class ConfigMarker(project: Project) : MarkerService<ConfigMarker.ConfigConfig>(project) {
    override val methodName = "config.get"

    override val icon = AllIcons.Actions.ShowCode

    override val tooltip = "Show config value"

    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    @Serializable
    data class ConfigConfig(
        @SerialName("config_name")
        val configName: String,

        @SerialName("view_mode")
        val viewMode: String,

        val value: String?,
    )

    override fun execCommand(keyName: String): DataResponse<ConfigConfig> {
        return execCommand(keyName, ConfigConfig.serializer())
    }

    override fun generatePopup(model: ConfigConfig): JComponent {
        return popupPanel {
            rowTitle("Конфиг:") {
                label(model.configName)
                    .monospace()
            }

            rowContent("Значение:") {
                if (model.value != null) {
                    textComponent(model.value, ViewMode.getByName(model.viewMode))
                } else {
                    label("Ошибка при отображение значения")
                }
            }
        }
    }
}
