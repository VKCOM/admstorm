package com.vk.admstorm.services

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
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

@Service(Service.Level.PROJECT)
class HastebinService {
    companion object {
        private val LOG = logger<HastebinService>()

        fun getInstance(project: Project) = project.service<HastebinService>()
    }

    private fun createHttpClient(): HttpClient {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .executor(ProcessIOExecutorService.INSTANCE)
            .build()
    }

    fun createHaste(data: String): String? {
        val hastLink = Env.data.services.getByKey("hastebin")?.url ?: return null
        LOG.info("Getting hastLink, hastLink is $hastLink")

        val httpClient = createHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$hastLink/documents"))
            .POST(HttpRequest.BodyPublishers.ofString(data))
            .timeout(Duration.ofSeconds(10)).build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            LOG.error(
                "Something wrong with Hastebin service:" +
                        " status code ${response.statusCode()}, error: ${response.body()}"
            )
            return null
        }

        val jsonResponse = Json.parseToJsonElement(response.body())
        if(jsonResponse !is JsonObject){
            LOG.error(
                "Something wrong with Hastebin service:" +
                        " status code ${response.statusCode()}, error: ${response.body()}"
            )
            return null
        }

        val value = jsonResponse["key"]?.jsonPrimitive?.content
        return "$hastLink/${value}"
    }
}
