package data.io.matrix.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Registration request for Matrix */
@Serializable
data class EmailRegistrationRequest(
    val auth: AuthenticationData,
    val password: String?,
    val username: String?,
    @SerialName("initial_device_display_name")
    val initialDeviceDisplayName: String
)