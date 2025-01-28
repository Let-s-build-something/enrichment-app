package data.io.matrix.auth

import kotlinx.serialization.Serializable

/** Request body for Matrix login */
@Serializable
data class EmailLoginRequest(
    val identifier: MatrixIdentifierData,
    val initialDeviceDisplayName: String,
    val password: String,
    val type: String
)