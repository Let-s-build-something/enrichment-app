package ui.login.sso

import kotlinx.serialization.Serializable

@Serializable
data class AugmySsoResponse(
    val userId: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Long? = null,
    val deviceId: String? = null,
)