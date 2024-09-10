package data.io

import data.io.identity_platform.ToolKitIdentityError

/** User signed in through Google Cloud. Either via Firebase or via HTTP */
data class CloudUserResponse(

    /** All available information about the user */
    val user: CloudUser? = null,

    /** Error response if there is any */
    val error: ToolKitIdentityError? = null
)