package data.io.social.network.conversation.matrix

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Metadata about a remote file. */
@Serializable
data class RoomMessageFileInfo(
    /** The mimetype of the file e.g. application/msword. */
    val mimetype: String? = null,

    /** The size of the file in bytes. */
    val size: Long? = null,

    /** Information on the encrypted thumbnail file if encrypted. */
    @SerialName("thumbnail_file")
    val thumbnailFile: MatrixEncryptedFile? = null,

    /** Metadata about the image referred to in thumbnail_url. */
    val thumbnailInfo: RoomMessageFileInfo? = null,

    /**  */
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null
)