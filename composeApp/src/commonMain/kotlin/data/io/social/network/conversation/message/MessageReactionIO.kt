package data.io.social.network.conversation.message

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import data.io.matrix.room.event.ConversationRoomMember
import database.AppRoomDatabase
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.serializers.LocalDateTimeIso8601Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** A singular reaction to a message */
@Entity(
    tableName = AppRoomDatabase.TABLE_MESSAGE_REACTION,
    // message may not yet exist
    /*foreignKeys = [
        ForeignKey(
            entity = ConversationMessageIO::class,
            parentColumns = ["id"],
            childColumns = ["message_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],*/
    indices = [Index(value = ["message_id"]), Index(value = ["event_id"], unique = true)]
)
@Serializable
data class MessageReactionIO @OptIn(ExperimentalUuidApi::class) constructor(

    @ColumnInfo("event_id")
    @PrimaryKey
    val eventId: String = Uuid.random().toString(),

    /** message content */
    val content: String? = null,

    @ColumnInfo("message_id")
    val messageId: String,

    /** Public id of the author of this message */
    @ColumnInfo("author_public_id")
    val authorPublicId: String? = null,

    @ColumnInfo("sent_at")
    @Serializable(with = LocalDateTimeIso8601Serializer::class)
    val sentAt: LocalDateTime? = null,
) {
    @Transient
    @Ignore
    var user: ConversationRoomMember? = null

    @Transient
    @Ignore
    var type: ReactionType = ReactionType.Add

    enum class ReactionType {
        Add,
        Remove
    }
}
