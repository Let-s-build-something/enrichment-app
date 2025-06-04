package data.io.matrix.auth

import kotlinx.serialization.Serializable

@Serializable
data class MatrixRegistrationRequest(
    val initialDeviceDisplayName: String
)