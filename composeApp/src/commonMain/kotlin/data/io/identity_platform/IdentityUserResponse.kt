package data.io.identity_platform

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/** Data response for user sign in/sign up */
@Serializable
data class IdentityUserResponse(

    /** type of the response */
    val kind: String = "",

    /** token for the user */
    val idToken: String = "",

    /** email of the user */
    val email: String = "",

    /** display name of the user */
    val displayName: String = "",

    /** refresh token for the user */
    val refreshToken: String = "",

    /** refresh token for the user */
    val userId: String = "",

    /** whether the user is registered or not */
    val registered: Boolean = true,

    /** time until the token expires */
    val expiresIn: Long = 0,

    /** local id of the user */
    val localId: String = "",

    /** profile picture of the user */
    val profilePicture: String = "",

    /** access token for the user */
    val oauthAccessToken: String = "",

    /** time until the access token expires */
    val oauthExpireIn: Int = 0,

    /** authorization code for the user */
    val oauthAuthorizationCode: String = "",

    /** pending credential for multi-factor authentication */
    val mfaPendingCredential: String = "",

    /** error if there is any */
    val error: ToolKitIdentityError? = null,

    /** time when the token expires in epoch seconds */
    val expiresAt: Long = Clock.System.now().epochSeconds + expiresIn
)