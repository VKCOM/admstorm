package com.vk.admstorm.admscript

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.vk.admstorm.AdmService
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.admscript.utils.DataResponse
import com.vk.admstorm.admscript.utils.DataResponseSerializer
import com.vk.admstorm.env.Env
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

abstract class AdmScript<TModel>(private val project: Project) : IService<TModel> {
    companion object {
        private val LOG = logger<AdmScript<Any>>()

        private val resultCache: Cache<String, String> = CacheBuilder.newBuilder()
            .weakKeys()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build()

    }

    abstract val methodName: String

    protected fun <TModel : Any> execCommand(keyName: String, serializer: KSerializer<TModel>): DataResponse<TModel> {
        val workingDir = "${Env.data.projectRoot}/${Env.data.phpSourceFolder}"
        val admScriptName = Env.data.admScriptName
        val admVersion = PluginManagerCore.getPlugin(AdmService.PLUGIN_ID)?.version

        val baseCommand = "php ${workingDir}/${admScriptName}"
        val command = "$baseCommand --method $methodName --key $keyName --version $admVersion"

        var isFromCache = true
        val cacheValue = resultCache.getIfPresent(keyName)
        val stdout = if (cacheValue == null) {
            val output = CommandRunner.runRemotely(
                project,
                command,
                command,
                workingDir,
                5_000,
            )

            if (output.exitCode == CommandRunner.FAILED_CODE) {
                return DataResponse(errorMessage = "An error occurred while sending data")
            }

            if (output.exitCode == CommandRunner.TIMEOUT_CODE) {
                return DataResponse(errorMessage = "Timeout occurred. Try again.")
            }

            val stdout = output.stdout
            if (output.exitCode != 0) {
                return DataResponse(errorMessage = stdout)
            }

            isFromCache = false
            stdout
        } else {
            cacheValue
        }

        return try {
            val response = Json.decodeFromString(DataResponseSerializer(serializer), stdout)
            if (response.errorMessage == null && !isFromCache) {
                resultCache.put(keyName, stdout)
            }

            response
        } catch (e: Exception) {
            LOG.warn("Error when decode the response:", e)
            return DataResponse(errorMessage = "Error when decode the response")
        }
    }

    abstract override fun execCommand(keyName: String): DataResponse<TModel>
}
