package com.vk.admstorm.ssh

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.wm.WindowManager
import com.intellij.remote.RemoteConnectionType
import com.intellij.remote.RemoteConnector
import com.jetbrains.plugins.remotesdk.RemoteSdkBundle
import com.jetbrains.plugins.remotesdk.console.RemoteConnectionSettingsForm
import com.jetbrains.plugins.remotesdk.console.RemoteConnectionUtil
import com.jetbrains.plugins.remotesdk.console.RemoteDataProducer
import com.jetbrains.plugins.remotesdk.console.SshConfigConnector
import java.awt.Component
import java.awt.MouseInfo
import java.awt.event.KeyEvent
import java.lang.reflect.Method
import java.util.function.Consumer

class RemoteDataProducerWrapper : RemoteDataProducer() {
    companion object {
        private val LOG = logger<RemoteDataProducerWrapper>()
        private val cls = RemoteDataProducer::class.java
    }

    private var project: Project? = null

    private lateinit var getProjectForServersSearchMethod: Method
    private lateinit var getEmptyConnectorsMessageMethod: Method

    init {
        try {
            getProjectForServersSearchMethod = cls.getDeclaredMethod("getProjectForServersSearch")
            getProjectForServersSearchMethod.isAccessible = true
            getEmptyConnectorsMessageMethod = cls.getDeclaredMethod("getEmptyConnectorsMessage")
            getEmptyConnectorsMessageMethod.isAccessible = true
        } catch (e: Exception) {
            LOG.warn("Unexpected exception while getDeclaredMethod for RemoteDataProducer", e)
        }
    }

    override fun withProject(project: Project): RemoteDataProducerWrapper {
        this.project = project
        super.withProject(project)
        return this
    }

    private fun getEmptyConnectorsMessageProxy(): String? = getEmptyConnectorsMessageMethod.invoke(this) as String?

    private fun getSuperProject(): Project? {
        val project = cls.getDeclaredField("myProject")
        project.isAccessible = true
        return project.get(this) as Project?
    }

    private fun getSuperComponentOwner(): Component? {
        val componentOwner = cls.getDeclaredField("myComponentOwner")
        componentOwner.isAccessible = true
        return componentOwner.get(this) as Component?
    }

    private fun getSuperActionEvent(): AnActionEvent? {
        val actionEvent = cls.getDeclaredField("myActionEvent")
        actionEvent.isAccessible = true
        return actionEvent.get(this) as AnActionEvent?
    }

    fun produceRemoteDataWithFirstConnector(
        id: String? = null,
        additionalData: String? = null,
        project: Project,
        consumer: Consumer<in RemoteConnector>,
    ) {
        val connector = getRemoteConnector(RemoteConnectionType.SSH_CONFIG, id, additionalData)
        if (connector != null && connector is SshConfigConnector) {
            consumer.accept(connector)
        } else {
            ApplicationManager.getApplication().invokeAndWait { selectFirstConnector(project, consumer) }
        }
    }

    /**
     * See [SshConfigConnector].
     */
    fun produceRemoteDataWithConnector(
        id: String? = null,
        additionalData: String? = null,
        project: Project,
        consumer: Consumer<in RemoteConnector>,
    ) {
        val connector = getRemoteConnector(RemoteConnectionType.SSH_CONFIG, id, additionalData)
        if (connector != null && connector is SshConfigConnector) {
            consumer.accept(connector)
        } else {
            ApplicationManager.getApplication().invokeAndWait { selectConnectorInPopup(project, consumer) }
        }
    }

    private fun selectFirstConnector(project: Project, consumer: Consumer<in RemoteConnector>) {
        val connectors = RemoteConnectionUtil.getUniqueRemoteConnectors(project)
        if (connectors.isEmpty()) {
            Messages.showWarningDialog(
                getSuperProject(), getEmptyConnectorsMessageProxy(),
                (NO_HOST_TO_CONNECT_SUPPLIER.get() as String)
            )
        } else if (connectors.size == 1 && connectors[0] === RemoteConnectionSettingsForm.NONE_CONNECTOR) {
            // do nothing
        } else {
            invokeLater {
                consumer.accept(connectors[1])
            }
        }
    }

    private fun selectConnectorInPopup(project: Project, consumer: Consumer<in RemoteConnector>) {
        val connectors = RemoteConnectionUtil.getUniqueRemoteConnectors(project)
        if (connectors.isEmpty()) {
            Messages.showWarningDialog(
                getSuperProject(), getEmptyConnectorsMessageProxy(),
                (NO_HOST_TO_CONNECT_SUPPLIER.get() as String)
            )
        } else if (connectors.size == 1 && connectors[0] === RemoteConnectionSettingsForm.NONE_CONNECTOR) {
            this.openSSHConfigurationsSettings()
        } else {
            connectors.sortWith { c1: RemoteConnector, c2: RemoteConnector ->
                when {
                    c1.type == RemoteConnectionType.NONE -> -1
                    c2.type == RemoteConnectionType.NONE -> 1
                    else -> c1.name.compareTo(c2.name)
                }
            }

            chooseConnectorWithConnectorConsumer(connectors, consumer)
        }
    }

    private fun chooseConnectorWithConnectorConsumer(
        connectors: List<RemoteConnector>,
        consumer: Consumer<in RemoteConnector>
    ) {
        val hostsStep = object : BaseListPopupStep<RemoteConnector>(
            RemoteSdkBundle.message("popup.title.select.host.to.connect"),
            connectors
        ) {
            override fun getTextFor(value: RemoteConnector): String {
                return if (value.type == RemoteConnectionType.NONE) {
                    "Edit SSH configurations..."
                } else {
                    value.name
                }
            }

            override fun onChosen(selected: RemoteConnector, finalChoice: Boolean): PopupStep<*>? {
                if (selected is SshConfigConnector) {
                    invokeLater {
                        consumer.accept(selected)
                    }
                } else {
                    this@RemoteDataProducerWrapper.openSSHConfigurationsSettings()
                }
                return FINAL_CHOICE
            }
        }

        val popup = JBPopupFactory.getInstance().createListPopup(hostsStep)
        val componentOwner = getSuperComponentOwner()
        val project = getSuperProject()
        val actionEvent = getSuperActionEvent()

        if (componentOwner != null) {
            popup.showInCenterOf(componentOwner)
            return
        }

        if (project == null) {
            popup.showInFocusCenter()
            return
        }

        if (actionEvent != null && actionEvent.inputEvent is KeyEvent) {
            popup.showInFocusCenter()
            return
        }

        popup.showInScreenCoordinates(
            WindowManager.getInstance().getIdeFrame(project)!!.component,
            MouseInfo.getPointerInfo().location
        )
    }

    private fun openSSHConfigurationsSettings() {
        invokeLater {
            ShowSettingsUtil.getInstance()
                .showSettingsDialog(this@RemoteDataProducerWrapper.project, "SSH Configurations")
        }
    }
}
