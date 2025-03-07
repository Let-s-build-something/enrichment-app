package data.io.matrix.auth.local

import kotlinx.serialization.Serializable

@Serializable
data class AuthItem(
    val homeserver: String? = null,

    val loginType: String? = null,

    /** the medium of login */
    val medium: String? = null,

    val expiresAtMsEpoch: Long? = null,

    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: String? = null,
    val address: String? = null,
    val password: String? = null,
    val pickleKey: String? = null,
    val deviceId: String?
)
