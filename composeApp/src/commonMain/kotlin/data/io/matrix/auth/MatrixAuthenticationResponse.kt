package data.io.matrix.auth

import kotlinx.serialization.Serializable

/** Successful registration response */
@Serializable
data class MatrixAuthenticationResponse(
    val accessToken: String? = null,
    val homeserver: String? = null,
    val userId: String? = null,
    val deviceId: String? = null,
    val refreshToken: String? = null,
    val expiresInMs: Long? = null
): MatrixAuthenticationPlan()