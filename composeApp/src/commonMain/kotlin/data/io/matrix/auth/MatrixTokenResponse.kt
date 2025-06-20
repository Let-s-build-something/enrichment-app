package data.io.matrix.auth

import kotlinx.serialization.Serializable

/** Response with the Matrix registration token */
@Serializable
data class MatrixTokenResponse(
    val sid: String,
    val submitUrl: String? = null
)