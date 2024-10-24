package data.io.social.username

import kotlinx.serialization.Serializable

/** Request for changing a username */
@Serializable
data class RequestUsernameChange(
    /** required field, the new username the user chose, can contain white spaces */
    val username: String
)