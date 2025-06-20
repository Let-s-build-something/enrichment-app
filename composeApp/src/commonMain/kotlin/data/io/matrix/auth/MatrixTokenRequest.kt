package data.io.matrix.auth

import kotlinx.serialization.Serializable

/** Request for registration token retrieval */
@Serializable
data class MatrixTokenRequest(
    val clientSecret: String?,
    val email: String,
    val sendAttempt: Int?
)