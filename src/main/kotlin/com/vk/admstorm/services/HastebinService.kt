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
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors

@Service(Service.Level.PROJECT)
class HastebinService {
    companion object {
        fun getInstance(project: Project) = project.service<HastebinService>()
    }

    private fun createHttpClient(): HttpClient {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .executor(Executors.newCachedThreadPool())
            .build()
    }

    fun createHaste(data: String): String? {
        val hastLink = Env.data.services.getByKey("hastebin")?.url ?: return ""

        val client = createHttpClient()
        val request = HttpRequest.newBuilder().uri(URI.create("$hastLink/documents"))
            .POST(HttpRequest.BodyPublishers.ofString(data)).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        val jsonResponse = Json.parseToJsonElement(response.body())
        if (jsonResponse is JsonObject) {
            val value = jsonResponse["key"]?.jsonPrimitive?.content
            return "$hastLink/${value}"
        }
        return null
    }
}
