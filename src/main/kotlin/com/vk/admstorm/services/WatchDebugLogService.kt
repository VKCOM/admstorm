package com.vk.admstorm.services

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.vk.admstorm.env.Env
import com.vk.admstorm.executors.WatchDebugLogCommandExecutor
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.utils.MyUtils.executeOnPooledThread

@Service(Service.Level.PROJECT)
class WatchDebugLogService(private val project: Project) : Disposable {
    companion object {
        const val PROPERTY_ID = "admstorm.watch.debug.log.running.state"
        fun getInstance(project: Project) = project.service<WatchDebugLogService>()
    }

    enum class State {
        RUNNING,
        STOPPED,
    }

    private var executor: WatchDebugLogCommandExecutor? = null

    fun isRunning() = state() != State.STOPPED

    fun toggle() {
        if (isRunning()) {
            stop()
        } else {
            start()
        }
    }

    fun start() {
        if (!isConnected()) {
            return
        }

        executor = WatchDebugLogCommandExecutor(project, "${Env.data.vkCommand} debug")

        executeOnPooledThread {
            if (executor!!.run()) {
                setWorking(true)
            }
        }
    }

    fun stop() {
        if (isRunning()) {
            executor?.stop()
        }
        setWorking(false)
    }

    fun showConsole() {
        executor?.showToolWindow()
    }

    fun state() = State.valueOf(PropertiesComponent.getInstance(project).getValue(PROPERTY_ID, State.STOPPED.name))

    fun setWorking(value: Boolean) {
        if (value) {
            setState(State.RUNNING)
        } else {
            setState(State.STOPPED)
        }
    }

    private fun setState(value: State) {
        PropertiesComponent.getInstance(project).setValue(PROPERTY_ID, value.name)

        project.messageBus.syncPublisher(WatchDebugLogListener.TOPIC).onUpdateState()
    }

    private fun isConnected() = SshConnectionService.getInstance(project).isConnectedOrWarning()

    override fun dispose() {
        executor?.dispose()
    }

    internal interface WatchDebugLogListener {
        companion object {
            val TOPIC = Topic.create("WatchDebugLogListener", WatchDebugLogListener::class.java)
        }

        fun onUpdateState() {}
    }
}
