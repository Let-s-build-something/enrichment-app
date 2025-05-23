package data.io.matrix.room.event

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import data.io.social.network.conversation.message.MediaIO
import data.io.user.NetworkItemIO
import database.AppRoomDatabase.Companion.TABLE_ROOM_MEMBER
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent

@Entity(TABLE_ROOM_MEMBER)
@Serializable
data class ConversationRoomMember(

    @ColumnInfo("user_id")
    val userId: String,

    val content: MemberEventContent,

    @ColumnInfo("display_name")
    val displayName: String? = content.displayName,

    @ColumnInfo("room_id")
    val roomId: String,

    val timestamp: Long?,

    val sender: UserId?,

    @PrimaryKey
    val id: String = "${roomId}_$userId"
) {
    fun toNetworkItem() = NetworkItemIO(
        publicId = id,
        userId = userId,
        displayName = displayName ?: UserId(userId).localpart,
        avatar = MediaIO(url = content.avatarUrl)
    )
}
