package data.io.social.network.conversation.message

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import base.utils.LinkUtils
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
@Entity(tableName = AppRoomDatabase.TABLE_CONVERSATION_MESSAGE)
@Serializable
data class ConversationMessageIO @OptIn(ExperimentalUuidApi::class) constructor(

    /** Message identifier */
    @PrimaryKey
    val id: String = Uuid.random().toString(), // default value due to Room ksp requirement

    /** message content */
    val content: String? = null,

    /** List of Urls of attached media to this message */
    val media: List<MediaIO>? = null,

    /** Audio url as a content of this message */
    @ColumnInfo("audio_url")
    val audioUrl: String? = null,

    /** Url asset of a gif */
    @ColumnInfo("gif_asset")
    val gifAsset: GifAsset? = null,

    /** Public id of the author of this message */
    @ColumnInfo("author_public_id")
    val authorPublicId: String? = null,

    /** List of reactions to this message */
    val reactions: List<MessageReactionIO>? = null,

    /** Whether preview should be shown for this message */
    @ColumnInfo("show_preview")
    val showPreview: Boolean? = true,

    /**
     * Content of message this message is anchored to.
     * It doesn't contain any [anchorMessage] itself.
     */
    @ColumnInfo("anchor_message")
    val anchorMessage: ConversationAnchorMessageIO? = null,

    @ColumnInfo("anchor_message_id")
    val anchorMessageId: String? = anchorMessage?.id,

    @ColumnInfo("parent_anchor_message_id")
    val parentAnchorMessageId: String? = anchorMessage?.anchorMessageId,

    /** Time of message being sent in ISO format */
    @ColumnInfo(name = "sent_at")
    @Serializable(with = DateTimeAsStringSerializer::class)
    val sentAt: LocalDateTime? = null,

    /**
     * State of this message. Generally, this information is sent only for the last item,
     * as it represents all of the messages above it
     */
    val state: MessageState? = null,

    /** List of timings of each keystroke in this message */
    val timings: List<Long>? = null,

    /** Local database use only, we don't need this information from API */
    @ColumnInfo(name = "conversation_id")
    var conversationId: String? = null,

    /** Local database use only, we don't need this information from API */
    var transcribed: Boolean? = null
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
        mediaUrls = media,
        audioUrl = audioUrl,
        gifAsset = gifAsset,
        authorPublicId = authorPublicId,
        anchorMessageId = anchorMessageId
    )

    /** Whether content contains any website url */
    val containsUrl: Boolean
        get() = showPreview == true
                && content != null
                && LinkUtils.urlRegex.containsMatchIn(content)
}
