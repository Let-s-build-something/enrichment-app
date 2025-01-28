package data.io.matrix.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request for registration token retrieval */
@Serializable
data class MatrixTokenRequest(
    @SerialName("client_secret")
    val clientSecret: String?,
    val email: String,
    @SerialName("send_attempt")
    val sendAttempts: Int?
)