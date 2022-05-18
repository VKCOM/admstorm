package com.vk.admstorm.services

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.vk.admstorm.executors.YarnWatchCommandExecutor
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.utils.MyUtils.invokeAfter
import java.beans.PropertyChangeSupport
import java.time.LocalTime

@Service
class YarnWatchService(private val myProject: Project) : Disposable {
    companion object {
        const val PROPERTY_ID = "admstorm.yarn.watch.running.state"
        fun getInstance(project: Project) = project.service<YarnWatchService>()
    }

    enum class State {
        RUNNING,
        STOPPED,
        WITH_ERRORS
    }

    private var setErrorsStateTime: LocalTime? = null
    private val executor = YarnWatchCommandExecutor(myProject, "FORCE_COLOR=true yarn watch")
    val changes = PropertyChangeSupport(this)

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

        invokeLater {
            executor.run()
            setWorking(true)
        }
    }

    fun stop() {
        if (isRunning()) {
            executor.stop()
        }
        setWorking(false)
    }

    fun showConsole() {
        executor.showToolWindow()
        clearErrorsState()
    }

    fun state() = State.valueOf(PropertiesComponent.getInstance(myProject).getValue(PROPERTY_ID, State.STOPPED.name))

    fun setErrorsState() {
        // Если окно не открыто, то бесконечно показываем
        // анимацию ошибки, если же открыто, то только 5
        // секунд.
        if (executor.isToolWindowVisible()) {
            invokeAfter(5000) {
                clearErrorsState()
            }
        }
        setState(State.WITH_ERRORS)
        setErrorsStateTime = LocalTime.now()
    }

    fun clearErrorsState() {
        if (state() == State.WITH_ERRORS) {
            setState(State.RUNNING)
        }
    }

    fun setWorking(value: Boolean) {
        if (value) {
            setState(State.RUNNING)
        } else {
            setState(State.STOPPED)
        }
    }

    private fun setState(value: State) {
        PropertiesComponent.getInstance(myProject).setValue(PROPERTY_ID, value.name)
        changes.firePropertyChange(PROPERTY_ID, "", value.name)
    }

    private fun isConnected() = SshConnectionService.getInstance(myProject).isConnectedOrWarning()

    override fun dispose() {
        executor.dispose()
    }
}
