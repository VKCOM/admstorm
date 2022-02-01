package com.vk.admstorm

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

abstract class BackgroundableTask(project: Project, title: String) : Task.Backgroundable(project, title, true) {
    protected lateinit var myIndicator: ProgressIndicator

    /**
     * Creates a progress step with a task to be completed.
     *
     * This is useful when you need to complete multiple tasks and track progress:
     *
     *    step("first task", 0.3) {}
     *    step("second task", 0.6) {}
     *    step("third task", 1) {}
     *
     * @param name if is not empty, then will be set as the indicator text
     * @param fraction progress value that will be set after the task is completed
     * @param block block to be executed
     *
     */
    protected fun step(name: String, fraction: Double, block: Runnable) {
        if (name.isNotEmpty()) myIndicator.text = name

        block.run()
        myIndicator.fraction = fraction
    }

    /**
     * See [step].
     */
    protected fun step(fraction: Double, block: Runnable) {
        step("", fraction, block)
    }

    override fun run(indicator: ProgressIndicator) {
        myIndicator = indicator
        run()
    }

    abstract fun run()
}
