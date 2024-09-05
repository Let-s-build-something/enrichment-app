package data.io.identity_platform

import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Refresh token response given by Google Cloud Identity platform */
@Serializable
data class IdentityRefreshToken(

    /** the Identity Platform refresh token provided in the request or a new refresh token */
    @SerialName("refresh_token")
    val refreshToken: String = "",

    /** an Identity Platform ID token */
    @SerialName("id_token")
    val idToken: String = "",

    /** the uid corresponding to the provided ID token */
    @SerialName("user_id")
    val userId: String = "",

    /** time until the token expires */
    @SerialName("expires_in")
    val expiresIn: Long = 0,

    /** time when the token expires in epoch seconds */
    @SerialName("expires_at")
    val expiresAt: Long = Clock.System.now().epochSeconds + expiresIn
)