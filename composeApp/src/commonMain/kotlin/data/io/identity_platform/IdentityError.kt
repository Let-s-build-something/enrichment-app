package data.io.identity_platform

import kotlinx.serialization.Serializable

/** Generic shell for Tool kit response */
@Serializable
data class ToolKitIdentityError(
    /** response code */
    val code: Int = -1,

    /** error message */
    val message: String? = null,

    /** list of all errors in detail */
    val errors: List<IdentityMessage> = listOf()
) {

    /** type of error */
    val type: IdentityMessageType?
        get() = IdentityMessageType.entries.find { it.name == message }
}