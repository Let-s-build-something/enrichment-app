package data.io.social.network.conversation.message

import androidx.room.PrimaryKey
import data.io.matrix.room.event.ConversationRoomMember
import data.io.social.network.conversation.giphy.GifAsset
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Anchor message to [ConversationMessageIO].
 * In other words, the original message to which the following message replies to
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class ConversationAnchorMessageIO(
    /** Message identifier */
    @PrimaryKey
    val id: String = Uuid.random().toString(),

    /** message content */
    val content: String? = null,

    /** List of Urls of attached media to this message */
    val mediaUrls: List<MediaIO>? = null,

    /** Audio url as a content of this message */
    val audioUrl: String? = null,

    /** Url asset of a gif */
    val gifAsset: GifAsset? = null,

    /** Public id of the author of this message */
    val authorPublicId: String? = null,

    /** Index within the whole data set ASC */
    val index: Int? = null,

    val anchorMessageId: String? = null,

    val user: ConversationRoomMember? = null
)
