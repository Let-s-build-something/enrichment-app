package data.io.social.network

import kotlinx.serialization.Serializable

/** successful response to an inclusion request */
@Serializable
data class CircleRequestResponse(
    /**
     * Whether the circling was processed
     * This means the recipient is public and was added to the social circle immediately
     */
    val isInclusionImmediate: Boolean?,

    /**
     * unique public identifier of the recipient user,
     * value propagated only if [isInclusionImmediate] is true
     */
    val userUid: String? = null
)