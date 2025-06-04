package base.utils

import io.ktor.client.request.HttpResponseData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.contentLength
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

data class NetworkConnectivity(
    val speed: NetworkSpeed?,
    val isNetworkAvailable: Boolean?,
) {
    val isStable: Boolean
        get() = isNetworkAvailable != false && (speed?.ordinal ?: NetworkSpeed.Moderate.ordinal) > NetworkSpeed.VerySlow.ordinal
}

enum class NetworkSpeed {
    VerySlow,
    Slow,
    Moderate,
    Good,
    Fast
}

suspend fun HttpResponse.speedInMbps(): Double {
    return withContext(Dispatchers.IO) {
        val contentSize = (contentLength().takeIf { it != 0L } ?: bodyAsBytes().size.toLong()) * 2 // optimism about the speed
        if(contentSize < 400_000) return@withContext 0.0
        val secondsTaken = (responseTime.timestamp - requestTime.timestamp) / 1_000.0

        withContext(Dispatchers.Default) {
            if (contentSize > 0 && secondsTaken > 0) {
                val speedBps = contentSize / secondsTaken // bps
                speedBps / 1_000_000.0 // Mbps
            } else 0.0
        }
    }
}

suspend fun HttpResponseData.speedInMbps(): Double {
    return withContext(Dispatchers.IO) {
        val contentSize = (this@speedInMbps.body.toString().toByteArray().size.toLong()) * 2 // optimism about the speed
        if(contentSize < 400_000) return@withContext 0.0
        val secondsTaken = (responseTime.timestamp - requestTime.timestamp) / 1_000.0

        withContext(Dispatchers.Default) {
            if (contentSize > 0 && secondsTaken > 0) {
                val speedBps = contentSize / secondsTaken // bps
                speedBps / 1_000_000.0 // Mbps
            } else 0.0
        }
    }
}
