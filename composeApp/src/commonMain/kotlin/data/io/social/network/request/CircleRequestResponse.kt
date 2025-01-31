package data.io.social.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** successful response to an inclusion request */
@Serializable
data class CircleRequestResponse(
    /**
     * Whether the circling was processed
     * This means the recipient is public and was added to the social circle immediately
     */
    @SerialName("is_inclusion_immediate")
    val isInclusionImmediate: Boolean?,

    /**
     * unique public identifier of the recipient user,
     * value propagated only if [isInclusionImmediate] is true
     */
    @SerialName("target_public_id")
    val targetPublicId: String? = null
)