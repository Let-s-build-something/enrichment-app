package data.io.matrix.room

import kotlinx.serialization.Serializable

@Serializable
data class MatrixEventContent(
    /** Required: The message being sent. */
    val body: String? = null,

    /** The formatted version of the body. This is required if format is specified. */
    val formattedBody: String? = null,

    /** Required: The type of message being sent. */
    val messageType: String? = null,

    /** The original filename of the uploaded file. */
    val filename: String? = null,

    /** Required if the file is unencrypted. The URL (typically mxc:// URI) to the image. */
    val url: String? = null,

    /** Required if the file is encrypted. Information on the encrypted file. */
    val file: MatrixEncryptedFile? = null,

    /** Metadata about the file referred to in url. */
    val info: RoomMessageFileInfo? = null
)
