package data.io.social.network.conversation

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import data.io.DateTimeAsStringSerializer
import data.io.social.network.conversation.giphy.GifAsset
import data.io.user.NetworkItemIO
import database.AppRoomDatabase
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Conversation entity representing a singular message within a conversation */
@Entity(tableName = AppRoomDatabase.ROOM_CONVERSATION_MESSAGE_TABLE)
@Serializable
data class ConversationMessageIO @OptIn(ExperimentalUuidApi::class) constructor(

    /** Message identifier */
    @PrimaryKey
    val id: String = Uuid.random().toString(), // default value due to Room ksp requirement

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

    /** List of reactions to this message */
    val reactions: List<MessageReactionIO>? = null,

    /** Identification of a message to which this message is anchored to, such as a reply */
    val anchorMessageId: String? = null,

    /**
     * Content of message this message is anchored to.
     * It doesn't contain any [anchorMessage] itself.
     */
    val anchorMessage: ConversationAnchorMessageIO? = null,

    /** Time of creation */
    @ColumnInfo(name = "created_at")
    @Serializable(with = DateTimeAsStringSerializer::class)
    val createdAt: LocalDateTime? = null,

    /**
     * State of this message. Generally, this information is sent only for the last item,
     * as it represents all of the messages above it
     */
    val state: MessageState? = null,

    /** Local database use only, we don't need this information from API */
    @ColumnInfo(name = "conversation_id")
    var conversationId: String? = null
) {

    /** User attached to this message */
    @Ignore
    @Transient
    var user: NetworkItemIO? = null

    /** Converts this message to an anchor message */
    @Ignore
    fun toAnchorMessage() = ConversationAnchorMessageIO(
        id = id,
        content = content,
        mediaUrls = mediaUrls,
        audioUrl = audioUrl,
        gifAsset = gifAsset,
        authorPublicId = authorPublicId
    )
}
