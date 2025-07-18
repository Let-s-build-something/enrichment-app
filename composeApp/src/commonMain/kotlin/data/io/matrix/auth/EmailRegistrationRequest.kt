package data.io.matrix.auth

import kotlinx.serialization.Serializable

/** Registration request for Matrix */
@Serializable
data class EmailRegistrationRequest(
    val auth: AuthenticationData,
    val password: String?,
    val username: String?,
    val deviceId: String,
    val initialDeviceDisplayName: String? = null
)