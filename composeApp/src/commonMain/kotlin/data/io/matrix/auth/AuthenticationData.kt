package data.io.matrix.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Additional authentication data for registration request to Matrix */
@Serializable
data class AuthenticationData(
    val session: String?,
    val type: String?,
    @SerialName("threepid_creds")
    val credentials: AuthenticationCredentials? = null,
    val response: String? = null,
    @SerialName("user_accepts")
    val userAccepts: List<String>? = null
)