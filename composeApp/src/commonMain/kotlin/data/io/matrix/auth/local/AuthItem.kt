package data.io.matrix.auth.local

import augmy.interactive.shared.utils.DateUtils
import data.io.social.UserConfiguration
import database.factory.SecretByteArray
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
    val databasePassword: SecretByteArray?,
    val address: String? = null,
    val password: String? = null,
    val pickleKey: String? = null,
    val deviceId: String?,

    // init-app info
    val displayName: String? = null,
    val tag: String? = null,
    val publicId: String? = null,
    val idToken: String?,
    val configuration: UserConfiguration? = null
) {

    val isExpired: Boolean
        get() = accessToken == null || (expiresAtMsEpoch?.minus(1_000L) ?: 0) < DateUtils.now.toEpochMilliseconds()

    val isFullyValid: Boolean
        get() = accessToken != null
                && idToken != null
                && homeserver != null
                && userId != null
}
