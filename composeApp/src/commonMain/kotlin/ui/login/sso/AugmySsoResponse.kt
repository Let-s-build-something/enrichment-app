package ui.login.sso

import kotlinx.serialization.Serializable

@Serializable
data class AugmySsoResponse(
    val matrixUserId: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val expiresIn: Long?
)