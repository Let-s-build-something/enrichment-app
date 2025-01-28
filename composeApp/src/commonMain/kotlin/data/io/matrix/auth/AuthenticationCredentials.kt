package data.io.matrix.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** additional credentials for Matrix registration */
@Serializable
data class AuthenticationCredentials(
    val sid: String,
    @SerialName("client_secret")
    val clientSecret: String
)