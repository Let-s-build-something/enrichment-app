package data.io.social.username

import kotlinx.serialization.Serializable

/** Response from request for a username change */
@Serializable
data class ResponseDisplayNameChange(
    /** isn't null in case a new tag was generated */
    val tag: String? = null,

    /** the new username */
    val displayName: String? = null,

    /** User id for the Matrix protocol */
    val matrixUserId: String? = null,

    /** Authorization token */
    val accessToken: String? = null
)