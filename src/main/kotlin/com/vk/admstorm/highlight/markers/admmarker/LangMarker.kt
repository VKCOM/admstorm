package com.vk.admstorm.highlight.markers.admmarker

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.vk.admstorm.admscript.utils.DataResponse
import com.vk.admstorm.highlight.markers.impl.MarkerService
import com.vk.admstorm.utils.UiDslBuilderUtils.monospace
import com.vk.admstorm.utils.extensions.isNotNullOrBlank
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.swing.JComponent

class LangMarker(project: Project) : MarkerService<LangMarker.LangConfig>(project) {
    override val methodName = "lang.get"

    override val tooltip = "Show lang value"

    override val icon = AllIcons.Actions.MatchCaseHovered

    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    @Serializable
    data class LangInfo(
        @SerialName("translator_name")
        val translatorName: String?,

        @SerialName("translator_url")
        val translatorUrl: String?,

        val date: String,

        val value: String,

        @SerialName("view_mode")
        val viewMode: String,
    )

    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    @Serializable
    data class LangConfig(
        @SerialName("key_name")
        val keyName: String,

        @SerialName("key_url")
        val keyUrl: String,

        val description: String?,

        val languages: Map<String, LangInfo>,
    )

    override fun execCommand(keyName: String): DataResponse<LangConfig> {
        return execCommand(keyName, LangConfig.serializer())
    }

    override fun generatePopup(model: LangConfig): JComponent {
        return popupPanel {
            rowTitle("Перевод:") {
                browserLink(model.keyName, model.keyUrl)
                    .monospace()
            }

            if (model.description.isNotNullOrBlank()) {
                rowContent("Описание:") {
                    description(model.description)
                }
            }

            for ((langName, langInfo) in model.languages) {
                val rowLangName = langName.replaceFirstChar { it.titlecase() }
                rowContent("$rowLangName:") {
                    val mode = ViewMode.getByName(langInfo.viewMode)
                    if (mode == ViewMode.SINGLE) {
                        description(langInfo.value)
                    } else {
                        browserLink("Невозможно отобразить значение", model.keyUrl)
                    }
                }.topGap(TopGap.SMALL)

                rowContent("Изменён:") {
                    if (langInfo.translatorName != null && langInfo.translatorUrl != null) {
                        browserLink(langInfo.translatorName, langInfo.translatorUrl)
                            .gap(RightGap.SMALL)
                    }

                    date(langInfo.date)
                }
            }
        }
    }
}
