package com.vk.admstorm.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.vk.admstorm.env.Env
import com.vk.admstorm.env.getByKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpResponse

@Service(Service.Level.PROJECT)
class HastebinService {
    companion object {
        fun getInstance(project: Project) = project.service<HastebinService>()
    }

    fun createHaste(data: String): String {
        val output = data.replace("\"", "\\\"").replace("$", "\\$")
        val hastLink = Env.data.services.getByKey("hastebin")?.url ?: return ""

        val client = HttpClient.newBuilder().build()
        val request = java.net.http.HttpRequest.newBuilder().uri(URI.create("$hastLink/documents"))
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(output)).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        val jsonResponse = Json.parseToJsonElement(response.body())
        if (jsonResponse is JsonObject) {
            val value = jsonResponse["key"]?.jsonPrimitive?.content
            return "$hastLink/${value}"
        }
        return ""
    }
}
