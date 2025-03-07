package data.io.matrix.auth

import kotlinx.serialization.Serializable

/** Request body for Matrix login */
@Serializable
data class EmailLoginRequest(
    val identifier: MatrixIdentifierData,
    val initialDeviceDisplayName: String,
    val password: String?,
    /** Whether our app supports refresh token - should be always true */
    val refreshToken: Boolean = true,
    val type: String,
    val deviceId: String?
)