package data.io.social.network.conversation.message

import androidx.room.Ignore
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
    val size: Long? = null,

    /** Local file path */
    @Ignore
    @Transient
    val path: String? = null
) {
    val isEmpty: Boolean
        get() = url.isNullOrBlank() || path.isNullOrBlank()

    override fun toString(): String {
        return "{" +
                "url: $url, " +
                "mimetype: $mimetype, " +
                "name: $name, " +
                "size: $size, " +
                "path: $path" +
                "}"
    }
}
