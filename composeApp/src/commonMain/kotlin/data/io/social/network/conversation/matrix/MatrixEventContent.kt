package data.io.social.network.conversation.matrix

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class MatrixEventContent(

) {

    @Serializable
    data class RoomAvatar(
        /** Required: The URL (typically mxc:// URI) to the image. */
        val url: String? = null,

        /** Required if the file is unencrypted. The URL (typically mxc:// URI) to the image. */
        val info: RoomMessageFileInfo? = null,
    )

    @Serializable
    data class RoomMessageEvent(
        /** Required: The message being sent. */
        val body: String? = null,

        /** The formatted version of the body. This is required if format is specified. */
        @SerialName("formatted_body")
        val formattedBody: String? = null,

        /** Required: The type of message being sent. */
        val msgtype: String? = null,

        /** The original filename of the uploaded file. */
        val filename: String? = null,

        /** Required if the file is unencrypted. The URL (typically mxc:// URI) to the image. */
        val url: String? = null,

        /** Required if the file is encrypted. Information on the encrypted file. */
        val file: MatrixEncryptedFile? = null,

        /** Metadata about the file referred to in url. */
        val info: RoomMessageFileInfo? = null
    )
}