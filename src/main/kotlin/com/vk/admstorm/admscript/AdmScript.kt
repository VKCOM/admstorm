package com.vk.admstorm.admscript

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.vk.admstorm.CommandRunner
import com.vk.admstorm.admscript.utils.DataResponse
import com.vk.admstorm.admscript.utils.DataResponseSerializer
import com.vk.admstorm.env.Env
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

abstract class AdmScript<TModel>(private val project: Project) : IService<TModel> {
    companion object {
        private val LOG = logger<AdmScript<Any>>()
    }

    abstract val methodName: String

    protected fun <TModel : Any> execCommand(keyName: String, serializer: KSerializer<TModel>): DataResponse<TModel> {
        val workingDir = "${Env.data.projectRoot}/${Env.data.phpSourceFolder}"
        val admScriptName = Env.data.admScriptName

        val baseCommand = "php ${workingDir}/${admScriptName}"
        val command = "$baseCommand --method $methodName --key $keyName"

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

        return try {
            Json.decodeFromString(DataResponseSerializer(serializer), stdout)
        } catch (e: Exception) {
            LOG.warn("Error when decode the response:", e)
            return DataResponse(errorMessage = "Error when decode the response")
        }
    }

    abstract override fun execCommand(keyName: String): DataResponse<TModel>
}
