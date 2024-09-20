package data.io.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response body after a creation of a user on our BE */
@Serializable
data class ResponseCreateUser(
    /** Current server time */
    @SerialName("server_time")
    val serverTime: Long? = null,

    /** generated public id of the user */
    @SerialName("public_id")
    val publicId: String? = null
)