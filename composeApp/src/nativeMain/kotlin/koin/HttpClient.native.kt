package koin

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun httpClient() = HttpClient(Darwin)
actual fun isConnectionException(cause: Throwable): Boolean = false