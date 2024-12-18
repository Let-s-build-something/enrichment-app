package data.io.social.network.conversation.giphy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Directory of images contained within [GiphyData] */
@Serializable
data class GiphyImages(
    /** Original image */
    val original: GiphyImage? = null,

    /** Original image fixed to certain width */
    @SerialName("fixed_width")
    val fixedWidth: GiphyImage? = null,

    /** Downsampled image fixed to certain width */
    @SerialName("fixed_width_downsampled")
    val fixedWidthDownsampled: GiphyImage? = null
)