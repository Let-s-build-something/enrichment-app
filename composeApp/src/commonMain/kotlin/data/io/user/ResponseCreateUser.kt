package data.io.user

import kotlinx.serialization.Serializable

/** Response body after a creation of a user on our BE */
@Serializable
data class ResponseCreateUser(
    /** Current server time */
    val serverTime: Long? = null,

    /** generated public id of the user */
    val publicId: String? = null
)