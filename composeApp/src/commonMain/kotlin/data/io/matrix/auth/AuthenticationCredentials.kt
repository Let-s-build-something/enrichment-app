package data.io.matrix.auth

import kotlinx.serialization.Serializable

/** additional credentials for Matrix registration */
@Serializable
data class AuthenticationCredentials(
    val sid: String,
    val clientSecret: String
)