package data.io.matrix.room.event

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
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

    @ColumnInfo("room_id")
    val roomId: String,

    val timestamp: Long?,

    val sender: UserId?,

    @PrimaryKey
    val id: String = "${roomId}_$userId"
)