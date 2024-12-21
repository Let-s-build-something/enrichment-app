package data.io.social.network.conversation

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import database.AppRoomDatabase.Companion.ROOM_EMOJI_SELECTION_TABLE

/** Object for storing information about emoji selection specific to a conversation and its author */
@Entity(tableName = ROOM_EMOJI_SELECTION_TABLE)
data class EmojiSelection(
    /** Description of the emoji, has to be unique */
    val name: String,

    /** Conversation identifier where this emoji has been selected */
    @ColumnInfo(name = "conversation_id")
    val conversationId: String? = null,

    /** UTF content of the emoji */
    val content: String? = null,

    /** Number of times this emojis has been selected */
    var count: Int = 0,

    @PrimaryKey
    /** Unique database identifier */
    val id: String = "${conversationId}_$name",
)