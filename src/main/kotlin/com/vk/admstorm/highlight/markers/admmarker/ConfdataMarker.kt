package com.vk.admstorm.highlight.markers.admmarker

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.RightGap
import com.vk.admstorm.admscript.utils.DataResponse
import com.vk.admstorm.utils.UiDslBuilderUtils.monospace
import com.vk.admstorm.utils.extensions.isNotNullOrBlank
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.swing.JComponent

class ConfdataMarker(project: Project) : MarkerService<ConfdataMarker.ConfdataConfig>(project) {
    override fun methodName() = "confdata.get"

    override fun getIcon() = AllIcons.Nodes.Editorconfig

    override fun getTooltip() = "Show confdata value"

    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    @Serializable
    data class ConfdataConfig(
        @SerialName("key_name")
        val keyName: String,

        @SerialName("key_url")
        val keyUrl: String,

        val description: String?,

        @SerialName("owner_name")
        val ownerName: String?,

        @SerialName("owner_url")
        val ownerUrl: String?,

        @SerialName("updater_url")
        val updaterUrl: String?,

        @SerialName("updater_name")
        val updaterName: String,

        @SerialName("updated_date")
        val updatedDate: String,

        @SerialName("value")
        val value: String,

        @SerialName("view_mode")
        val viewMode: String,
    )

    override fun execCommand(keyName: String): DataResponse<ConfdataConfig> {
        return execCommand(keyName, ConfdataConfig.serializer())
    }

    override fun generatePopup(model: ConfdataConfig): JComponent {
        return popupPanel {
            rowTitle("Ключ:") {
                browserLink(model.keyName, model.keyUrl)
                    .monospace()
            }

            if (model.description.isNotNullOrBlank()) {
                rowContent("Описание:") {
                    description(model.description)
                }
            }

            if (model.ownerName != null && model.ownerUrl != null) {
                rowContent("Автор:") {
                    browserLink(model.ownerName, model.ownerUrl)
                }
            }

            rowContent("Изменён:") {
                if (model.updaterUrl != null) {
                    browserLink(model.updaterName, model.updaterUrl)
                        .gap(RightGap.SMALL)
                } else {
                    label(model.updaterName)
                        .gap(RightGap.SMALL)
                }

                date(model.updatedDate)
            }

            rowContent("Значение:") {
                textComponent(model.value, ViewMode.getByName(model.viewMode))
            }
        }
    }
}
