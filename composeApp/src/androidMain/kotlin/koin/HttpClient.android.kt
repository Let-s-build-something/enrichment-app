package koin

import io.ktor.client.HttpClient

internal actual fun httpClient(): HttpClient = HttpClient()