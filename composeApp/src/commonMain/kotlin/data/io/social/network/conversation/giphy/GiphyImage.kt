package data.io.social.network.conversation.giphy

import kotlinx.serialization.Serializable

/** Singular Giphy image information */
@Serializable
data class GiphyImage(
    /** Variable height */
    val height: Int? = null,

    /** Variable width */
    val width: Int? = null,

    /** Url to the image */
    val url: String? = null
)