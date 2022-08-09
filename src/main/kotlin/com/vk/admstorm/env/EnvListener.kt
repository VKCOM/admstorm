package com.vk.admstorm.env

import com.intellij.util.messages.Topic

interface EnvListener {
    companion object {
        val TOPIC = Topic.create("EnvListener", EnvListener::class.java)
    }

    fun resolveChanged() {}
}
