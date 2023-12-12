package com.vk.admstorm.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
/*import com.vk.admstorm.CommandRunner
import com.vk.admstorm.env.Env*/
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpResponse

@Service(Service.Level.PROJECT)
class HastebinService(private val project: Project) {
    companion object {
        fun getInstance(project: Project) = project.service<HastebinService>()
    }

    fun createHaste(data: String): String {
        val output = data.replace("\"", "\\\"").replace("$", "\\$")

        val client = HttpClient.newBuilder().build()
        val request = java.net.http.HttpRequest.newBuilder().uri(URI.create("secret"))
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(output)).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        val jsonResponse = Json.parseToJsonElement(response.body())
        if (jsonResponse is JsonObject) {
            val value = jsonResponse["key"]?.jsonPrimitive?.content
            return "secret/${value}"
        }
        return ""
        /*
                return CommandRunner.runRemotely(project, "echo \"$output\" | ${Env.data.pasteBinCommand}").stdout*/
    }
}
