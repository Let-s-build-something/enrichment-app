package data.io.social.network.conversation.giphy

import kotlinx.serialization.Serializable

/** Response equivalent to a single page of Giphy results */
@Serializable
data class GiphyPageResponse(
    /** Concrete information about a Giphy */
    val data: List<GiphyData>? = null,

    /** Information about current pagination */
    val pagination: GiphyPaginationInfo? = null
)