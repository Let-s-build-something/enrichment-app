package data.io.base

import kotlinx.serialization.Serializable

/**
 * Base error response message from API request, which will be sent in case something goes wrong in the BE
 */
@Serializable
open class BaseErrorMessage(

    /** Enum internal code. Such as INCORRECT_PASSWORD, DUPLICATE, etc. */
    val code: String,

    /** Developer readable message. */
    val rationale: String? = null
)