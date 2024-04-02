package com.vk.admstorm.ssh

import com.intellij.ssh.SshException

object SshHandler {
    inline fun <reified T> handle(call: () -> T): T {
        return try {
            call()
        } catch (e: SshException) {
          throw  unwrap(e)
        }
    }

    fun unwrap(ex: Exception): Throwable {
        var cause = ex.cause
        while(cause?.cause != null) {
            cause = cause.cause
        }
        return cause ?: ex
    }
}
