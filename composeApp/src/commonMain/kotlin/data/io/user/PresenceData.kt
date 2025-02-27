package data.io.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import database.AppRoomDatabase.Companion.TABLE_PRESENCE_EVENT
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.events.m.PresenceEventContent

@Entity(TABLE_PRESENCE_EVENT)
@Serializable
data class PresenceData(
    @PrimaryKey
    @ColumnInfo("user_id_full")
    val userIdFull: String,
    val content: PresenceEventContent? = null
)
