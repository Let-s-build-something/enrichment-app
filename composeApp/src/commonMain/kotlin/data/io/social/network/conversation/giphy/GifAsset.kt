package data.io.social.network.conversation.giphy

import kotlinx.serialization.Serializable

/** Bundle of information about a singular gif asset */
@Serializable
data class GifAsset(
    /** Small variation url, primarily for phones. Gif equivalent is "fixed_width_downsampled" */
    val fixedWidthSmall: String? = null,

    /** Original file fixed to a certain width. Gif equivalent is "fixed_width" */
    val fixedWidthOriginal: String? = null,

    /** Original gif url */
    val original: String? = null,

    /** Description of the gif */
    val description: String? = null
) {
    constructor(singleUrl: String) : this(
        fixedWidthSmall = singleUrl,
        fixedWidthOriginal = singleUrl,
        original = singleUrl
    )
}