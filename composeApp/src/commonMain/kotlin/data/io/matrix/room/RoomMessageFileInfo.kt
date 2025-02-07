package data.io.matrix.room

import kotlinx.serialization.Serializable

/** Metadata about a remote file. */
@Serializable
data class RoomMessageFileInfo(
    /** The mimetype of the file e.g. application/msword. */
    val mimetype: String? = null,

    /** The size of the file in bytes. */
    val size: Int? = null,

    /** Information on the encrypted thumbnail file if encrypted. */
    val thumbnailFile: MatrixEncryptedFile? = null,

    /** Metadata about the image referred to in thumbnail_url. */
    val thumbnailInfo: RoomMessageFileInfo? = null,

    /** url of the thumbnail image */
    val thumbnailUrl: String? = null
)
