package data.io.social.username

import kotlinx.serialization.Serializable

/** Request for changing user properties */
@Serializable
data class RequestUserPropertiesChange(
    /** required field, the new display name the user chose, can contain white spaces */
    val displayName: String
)