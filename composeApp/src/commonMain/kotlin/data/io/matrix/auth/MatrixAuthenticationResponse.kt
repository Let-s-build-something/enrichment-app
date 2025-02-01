package data.io.matrix.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Successful registration response */
@Serializable
data class MatrixAuthenticationResponse(
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("home_server")
    val homeServer: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("expires_in_ms")
    val expiresInMs: Long? = null
): MatrixAuthenticationPlan()