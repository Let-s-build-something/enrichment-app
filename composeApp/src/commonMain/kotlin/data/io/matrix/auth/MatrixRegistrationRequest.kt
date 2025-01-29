package data.io.matrix.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatrixRegistrationRequest(
    @SerialName("initial_device_display_name")
    val initialDeviceDisplayName: String
)