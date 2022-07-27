package com.vk.admstorm.highlight.markers

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.ui.awt.RelativePoint
import com.vk.admstorm.highlight.markers.admmarker.IMarker
import com.vk.admstorm.highlight.markers.admmarker.IMarkerBrowser
import com.vk.admstorm.highlight.markers.admmarker.MarkerService
import com.vk.admstorm.utils.MyUtils.executeOnPooledThread
import java.awt.event.MouseEvent

class ServiceGutterIconNavigationHandler(private val keyName: String, private val marker: IMarker) :
    GutterIconNavigationHandler<PsiElement> {

    override fun navigate(e: MouseEvent, elt: PsiElement) {
        when (marker) {
            is MarkerService<*> -> {
                popup(e, marker)
            }

            is IMarkerBrowser   -> {
                browser(marker)
            }
        }
    }

    private fun browser(marker: IMarkerBrowser) {
        BrowserUtil.browse(marker.generateUrl(keyName))
    }

    private fun <T> popup(e: MouseEvent, service: MarkerService<T>) {
        val builder = JBPopupFactory.getInstance()
        val point = RelativePoint(e)

        val loader = builder
            .createComponentPopupBuilder(service.loader(), null)
            .createPopup()

        loader.show(point)

        executeOnPooledThread {
            val response = service.execCommand(keyName)

            val popup = when {
                response.errorMessage != null -> {
                    service.errorMessage(response.errorMessage)
                }

                response.value == null        -> {
                    service.errorMessage("An error occurred while processing the data")
                }

                else                          -> {
                    service.generatePopup(response.value)
                }
            }

            invokeLater {
                if (loader.isVisible) {
                    loader.dispose()

                    builder
                        .createComponentPopupBuilder(popup, null)
                        .createPopup()
                        .show(point)
                }
            }
        }
    }
}
