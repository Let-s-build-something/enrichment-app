package data.io.matrix.auth.local

import kotlinx.serialization.Serializable

@Serializable
data class AuthItem(
    val homeserver: String?,

    val loginType: String?,

    /** the medium of login */
    val medium: String?,

    val expiresAtMsEpoch: Long?,

    val accessToken: String?,
    val refreshToken: String?,
    val userId: String?,
    val address: String?,
    val password: String?,
)
