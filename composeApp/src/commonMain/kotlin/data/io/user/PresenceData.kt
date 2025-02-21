package data.io.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import data.io.matrix.room.event.content.PresenceEventContent
import database.AppRoomDatabase.Companion.TABLE_PRESENCE_EVENT
import kotlinx.serialization.Serializable

@Entity(TABLE_PRESENCE_EVENT)
@Serializable
data class PresenceData(
    @PrimaryKey
    @ColumnInfo("user_id_full")
    val userIdFull: String,
    val content: PresenceEventContent? = null
)
