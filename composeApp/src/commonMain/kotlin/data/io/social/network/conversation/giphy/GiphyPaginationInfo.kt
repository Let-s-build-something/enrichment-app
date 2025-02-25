package data.io.social.network.conversation.giphy

import kotlinx.serialization.Serializable

/** Information about current pagination */
@Serializable
data class GiphyPaginationInfo(
    /** Position in pagination. */
    val offset: Int? = null,

    /** Total number of items returned. */
    val count: Int? = null,

    /** Total number of items available (not returned on every endpoint). */
    val totalCount: Int? = null
)