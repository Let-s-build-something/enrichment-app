package data.io.matrix.auth

import kotlinx.serialization.Serializable

/** Request body for Matrix token refresh */
@Serializable
data class RefreshTokenRequest(val refreshToken: String)