package com.vk.admstorm.ssh

import com.intellij.openapi.util.SystemInfo
import java.io.File

object OpenSCProviderDetector {
    private val LINUX_PATHS = arrayOf(
        "/usr/lib/x86_64-linux-gnu/opensc-pkcs11.so",
        "/usr/lib/x86_64-linux-gnu/libykcs11.so",
    )
    private val MAC_PATHS = arrayOf(
        "/usr/local/lib/opensc-pkcs11.so",
        "/usr/local/lib/libykcs11.dylib",
    )

    private fun detectForPaths(pathsList: Array<String>): String? {
        for (p in pathsList) {
            val f = File(p)
            if (f.exists()) {
                return f.path
            }
        }

        return null
    }

    fun detectPath(): String? {
        return when {
            SystemInfo.isMac   -> detectForPaths(MAC_PATHS)
            SystemInfo.isLinux -> detectForPaths(LINUX_PATHS)
            else               -> null
        }
    }
}
