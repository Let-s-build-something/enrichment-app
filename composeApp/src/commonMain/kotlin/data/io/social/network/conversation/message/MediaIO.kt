package data.io.social.network.conversation.message

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import database.AppRoomDatabase
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

/**
 * Information about a single media
 */
@Entity(
    tableName = AppRoomDatabase.TABLE_MEDIA,
    /*foreignKeys = [
        ForeignKey(
            entity = ConversationMessageIO::class,
            parentColumns = ["id"],
            childColumns = ["message_id"],
            onDelete = CASCADE
        )
    ],*/
    indices = [Index(value = ["message_id"]), Index(value = ["url"])]
)
@Serializable
data class MediaIO @OptIn(ExperimentalUuidApi::class) constructor(
    /** Access url for the media. Can be encrypted. */
    val url: String? = null,

    /** Type of media. Only generally reliable. */
    val mimetype: String? = null,

    /** The original file name */
    val name: String? = null,

    /** Size in bytes of the media */
    val size: Long? = null,

    /** Message this media belongs to */
    @ColumnInfo("message_id")
    val messageId: String? = null,

    @ColumnInfo("conversation_id")
    val conversationId: String? = null,

    /** Local file path */
    val path: String? = null,

    @PrimaryKey()
    val id: String = "${url}_${messageId}",
) {
    @get:Ignore
    val isEmpty: Boolean
        get() = url.isNullOrBlank() && path.isNullOrBlank()

    override fun toString(): String {
        return "{" +
                "url: $url, " +
                "mimetype: $mimetype, " +
                "name: $name, " +
                "size: $size, " +
                "path: $path" +
                "}"
    }
}
