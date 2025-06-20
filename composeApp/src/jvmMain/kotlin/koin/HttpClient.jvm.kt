package koin

import io.ktor.client.HttpClient
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

internal actual fun httpClient(): HttpClient = HttpClient()
actual fun isConnectionException(cause: Throwable): Boolean {
    return when (cause) {
        is SSLHandshakeException -> true        // SSL/TLS handshake failed
        is SocketTimeoutException -> true       // Connection timed out
        is ConnectException -> true            // Failed to connect to server
        is UnknownHostException -> true        // DNS resolution failed
        is NoRouteToHostException -> true      // No route to host
        else -> false
    }
}