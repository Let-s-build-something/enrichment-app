package data.io.social.username

import kotlinx.serialization.Serializable

/** Response from request for a username change */
@Serializable
data class ResponseUsernameChange(
    /** isn't null in case a new tag was generated */
    val tag: String? = null,

    /** the new username */
    val username: String? = null
)