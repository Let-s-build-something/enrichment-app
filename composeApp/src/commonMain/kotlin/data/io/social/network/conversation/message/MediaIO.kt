package data.io.social.network.conversation.message

import kotlinx.serialization.Serializable

/** Information about a single media */
@Serializable
data class MediaIO(
    /** Access url for the media. Can be encrypted. */
    val url: String? = null,

    /** Type of media. Only generally reliable. */
    val mimetype: String? = null,

    /** The original file name */
    val name: String? = null,

    /** Size in bytes of the media */
    val size: Int? = null
)