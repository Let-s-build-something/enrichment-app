package data.io.identity_platform

import kotlinx.serialization.Serializable

/** More generic information about response from Tool kit Identity */
@Serializable
data class IdentityMessage(

    /** domain of the message */
    val domain: String = "",

    /** type of the message */
    val message: IdentityMessageType? = null,

    /** message from the server, such as "invalid" */
    val reason: String = ""
)