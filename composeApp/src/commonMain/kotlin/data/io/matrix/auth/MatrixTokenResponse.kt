package data.io.matrix.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response with the Matrix registration token */
@Serializable
data class MatrixTokenResponse(
    val sid: String,
    @SerialName("submit_url")
    val submitUrl: String? = null
)