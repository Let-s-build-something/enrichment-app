package data.io.social.network.conversation

import androidx.room.Ignore
import androidx.room.PrimaryKey
import data.io.social.network.conversation.giphy.GifAsset
import data.io.user.NetworkItemIO
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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
    val mediaUrls: List<String>? = null,

    /** Audio url as a content of this message */
    val audioUrl: String? = null,

    /** Url asset of a gif */
    val gifAsset: GifAsset? = null,

    /** Public id of the author of this message */
    val authorPublicId: String? = null,

    /** Index within the whole data set */
    val index: Int? = null
) {
    /** User attached to this message */
    @Ignore
    @Transient
    var user: NetworkItemIO? = null
}
