package koin

import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.encodedPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** Tools for developers to work with ktor */
object DeveloperUtils {

    data class HttpLogData(
        val id: String = "",
        val httpCalls: MutableList<HttpCall> = mutableListOf()
    )

    data class HttpCall(
        var id: String = "",
        var method: HttpMethod? = null,
        var url: String? = null,
        var headers: List<String>? = null,
        var responseSeconds: Int? = null,
        var responseBody: String? = null,
        var requestBody: String? = null,
        var responseCode: Int? = null
    ) {
        val createdAt: Instant = Clock.System.now()

        /** updates the call with [other] http call data, but maintains existing data */
        fun update(other: HttpCall): HttpCall {
            return this.apply {
                responseBody = other.responseBody ?: responseBody
                requestBody = other.requestBody ?: requestBody
                responseSeconds = other.responseSeconds ?: responseSeconds
                responseCode = other.responseCode ?: responseCode
                method = other.method ?: method
                url = other.url ?: url
                headers = other.headers ?: headers
            }
        }
    }

    private val json = Json { prettyPrint = true }
    private suspend fun formatJson(jsonString: String): String {
        return withContext(Dispatchers.Default) {
            try {
                val jsonElement: JsonElement = Json.parseToJsonElement(jsonString)
                json.encodeToString(jsonElement)
            }catch (e: Exception) {
                jsonString
            }
        }
    }

    suspend fun processRequest(request: HttpRequestBuilder): HttpCall? {
        return withContext(Dispatchers.Default) {
            val headers = mutableListOf<String>()
            var id: String? = null

            request.headers.entries().toList().sortedBy { it.key }.forEach { (key, values) ->
                val placeholder = if(key == HttpHeaders.Authorization || key == HttpHeaders.IdToken) {
                    values.firstOrNull()?.take(4) + "..." + values.firstOrNull()?.takeLast(4)
                } else null
                headers.add("$key: ${placeholder ?: values.joinToString("; ")}")
                if(key == HttpHeaders.XRequestId) id = values.firstOrNull()
            }

            println("developer_utils request, url: ${request.url}")
            if(id == null) return@withContext null
            HttpCall(
                headers = headers,
                requestBody = formatJson((request.body as? TextContent)?.text ?: ""),
                url = request.url.encodedPath,
                method = request.method,
                id = id ?: "No id found"
            )
        }
    }

    suspend fun processResponse(response: HttpResponse): HttpCall? {
        return withContext(Dispatchers.Default) {
            println("developer_utils response, headers: ${response.request.headers.entries()}, ${response.headers.entries()}")
            response.request.headers.entries().find {
                it.key == HttpHeaders.XRequestId
            }?.value?.firstOrNull()?.let { id ->
                HttpCall(
                    id = id,
                    responseBody = formatJson(response.body()),
                    responseSeconds = response.responseTime.seconds - response.requestTime.seconds,
                    responseCode = response.status.value
                )
            }
        }
    }
}