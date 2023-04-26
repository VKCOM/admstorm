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

class PartMarker(project: Project) : MarkerService<PartMarker.PartConfig>(project) {
    override fun methodName() = "part.get"

    override fun getIcon() = AllIcons.Nodes.Editorconfig

    override fun getTooltip() = "Show part value"

    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    @Serializable
    data class User(
        val name: String,
        val url: String,
    )

    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    @Serializable
    data class ConfdataValue(
        val part: String,
        val exclusions: List<User>?,
        val uids: List<User>?,
    )

    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    @Serializable
    data class PartConfig(
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
        val value: ConfdataValue,
    )

    override fun execCommand(keyName: String): DataResponse<PartConfig> {
        return execCommand(keyName, PartConfig.serializer())
    }

    override fun generatePopup(model: PartConfig): JComponent {
        return popupPanel {
            rowTitle("Ручка:") {
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

            val value = model.value
            rowContent("Процент:") {
                label(value.part)
                    .bold()
            }

            if (value.uids != null) {
                rowContent("Всегда:") {
                    value.uids.forEach {
                        browserLink(it.name, it.url)
                            .gap(RightGap.SMALL)
                    }
                }
            }

            if (value.exclusions != null) {
                rowContent("Никогда:") {
                    value.exclusions.forEach {
                        browserLink(it.name, it.url)
                            .gap(RightGap.SMALL)
                    }
                }
            }
        }
    }
}
