package data.io.identity_platform

import kotlinx.serialization.Serializable

/** Data response for user sign in/sign up */
@Serializable
data class IdentityUserResponse(

    /** type of the response */
    val kind: String = "",

    /** token for the user */
    val idToken: String? = null,

    /** email of the user */
    val email: String = "",

    /** refresh token for the user */
    val refreshToken: String = "",

    /** time until the token expires */
    val expiresIn: Long = 0,

    /** local id of the user */
    val localId: String? = null,

    /** error if there is any */
    val error: ToolKitIdentityError? = null,
)