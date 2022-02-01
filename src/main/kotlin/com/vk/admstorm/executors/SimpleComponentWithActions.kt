package com.vk.admstorm.executors

import com.intellij.openapi.ui.ComponentWithActions
import javax.swing.JComponent

class SimpleComponentWithActions(toolbarContext: JComponent?, component: JComponent) : ComponentWithActions.Impl(
    null, null,
    toolbarContext,
    null, component
)
