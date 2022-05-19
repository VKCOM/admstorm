package com.vk.admstorm.ssh

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopupStep
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
        private val LOG = Logger.getInstance(RemoteDataProducerWrapper::class.java)
        private val cls: Class<RemoteDataProducer> = RemoteDataProducer::class.java
    }

    private var project: Project? = null

    override fun withProject(project: Project): RemoteDataProducerWrapper {
        this.project = project
        super.withProject(project)
        return this
    }

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

    private fun getEmptyConnectorsMessageProxy(): String? = getEmptyConnectorsMessageMethod.invoke(this) as String?

    private fun getSuperProject(): Project? {
        val myProject = cls.getDeclaredField("myProject")
        myProject.isAccessible = true
        return myProject.get(this) as Project?
    }

    private fun getSuperComponentOwner(): Component? {
        val myComponentOwner = cls.getDeclaredField("myComponentOwner")
        myComponentOwner.isAccessible = true
        return myComponentOwner.get(this) as Component?
    }

    private fun getSuperActionEvent(): AnActionEvent? {
        val myActionEvent = cls.getDeclaredField("myActionEvent")
        myActionEvent.isAccessible = true
        return myActionEvent.get(this) as AnActionEvent?
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
     * [SshConfigConnector]
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
        val sdkHomesStep: ListPopupStep<*> = object : BaseListPopupStep<RemoteConnector>(
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

        val popup = JBPopupFactory.getInstance().createListPopup(sdkHomesStep)
        val myComponentOwner = getSuperComponentOwner()
        val myProject = getSuperProject()
        val myActionEvent = getSuperActionEvent()

        if (myComponentOwner != null) {
            popup.showInCenterOf(myComponentOwner)
        } else if (myProject != null) {
            if (myActionEvent != null && myActionEvent.inputEvent is KeyEvent) {
                popup.showInFocusCenter()
            } else {
                popup.showInScreenCoordinates(
                    WindowManager.getInstance().getIdeFrame(myProject)!!.component,
                    MouseInfo.getPointerInfo().location
                )
            }
        } else {
            popup.showInFocusCenter()
        }
    }

    private fun openSSHConfigurationsSettings() {
        invokeLater {
            ShowSettingsUtil.getInstance()
                .showSettingsDialog(this@RemoteDataProducerWrapper.project, "SSH Configurations")
        }
    }
}
