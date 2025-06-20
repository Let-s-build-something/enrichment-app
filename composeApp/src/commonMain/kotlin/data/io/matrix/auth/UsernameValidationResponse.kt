package data.io.matrix.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Matrix username validation error response */
@Serializable
data class UsernameValidationResponse(
    val error: String? = null,
    @SerialName("errcode")
    val code: String? = null
)