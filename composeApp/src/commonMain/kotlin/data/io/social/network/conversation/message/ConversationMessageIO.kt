package data.io.social.network.conversation.message

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import data.io.matrix.room.event.ConversationRoomMember
import database.AppRoomDatabase
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.serializers.LocalDateTimeIso8601Serializer
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod
import ui.conversation.components.experimental.gravity.GravityData
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

    val gravityData: GravityData? = null,

    @ColumnInfo("anchor_message_id")
    val anchorMessageId: String? = anchorMessage?.id,

    @ColumnInfo("parent_anchor_message_id")
    val parentAnchorMessageId: String? = anchorMessage?.anchorMessageId,

    /** Time of message being sent in ISO format */
    @ColumnInfo(name = "sent_at")
    @Serializable(with = LocalDateTimeIso8601Serializer::class)
    val sentAt: LocalDateTime? = null,

    /**
     * State of this message. Generally, this information is sent only for the last item,
     * as it represents all of the messages above it
     */
    val state: MessageState? = null,

    /** List of timings of each keystroke in this message */
    val timings: List<Long>? = null,
    var transcribed: Boolean? = null,

    @ColumnInfo(name = "conversation_id")
    var conversationId: String? = null,

    val verification: VerificationRequestInfo? = null,

    val user: ConversationRoomMember? = null
) {

    @Serializable
    data class VerificationRequestInfo(
        val fromDeviceId: String,
        val methods: Set<VerificationMethod>,
        val to: String
    )

    /** Converts this message to an anchor message */
    @Ignore
    fun toAnchorMessage() = ConversationAnchorMessageIO(
        id = id,
        content = content,
        mediaUrls = media,
        authorPublicId = authorPublicId,
        anchorMessageId = anchorMessageId
    )

    @Ignore
    fun update(other: ConversationMessageIO): ConversationMessageIO {
        return this.copy(
            content = other.content ?: content,
            media = other.media ?: media,
            authorPublicId = other.authorPublicId ?: authorPublicId,
            reactions = other.reactions ?: reactions,
            showPreview = other.showPreview ?: showPreview,
            anchorMessage = other.anchorMessage ?: anchorMessage,
            anchorMessageId = other.anchorMessageId ?: anchorMessageId,
            parentAnchorMessageId = other.parentAnchorMessageId ?: parentAnchorMessageId,
            sentAt = other.sentAt ?: sentAt,
            state = other.state ?: state,
            timings = other.timings ?: timings,
            conversationId = other.conversationId ?: conversationId,
            transcribed = other.transcribed ?: transcribed,
            verification = other.verification ?: verification,
        )
    }

    fun relatesTo(): RelatesTo? {
        return when {
            parentAnchorMessageId != null -> RelatesTo.Thread(
                replyTo = if(anchorMessageId != null) RelatesTo.ReplyTo(EventId(anchorMessageId)) else null,
                eventId = EventId(parentAnchorMessageId)
            )
            anchorMessageId != null -> RelatesTo.Reply(
                replyTo = RelatesTo.ReplyTo(EventId(anchorMessageId))
            )
            else -> null
        }
    }
}
