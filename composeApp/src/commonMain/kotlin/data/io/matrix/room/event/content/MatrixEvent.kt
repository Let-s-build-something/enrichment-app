package data.io.matrix.room.event.content

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import database.AppRoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.EventContent
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import kotlin.uuid.ExperimentalUuidApi

@Entity(tableName = AppRoomDatabase.Companion.TABLE_ROOM_EVENT)
@OptIn(ExperimentalUuidApi::class)
@Serializable
open class MatrixEvent(
    val content: String,
    val eventId: EventId,
    val sender: UserId,
    @ColumnInfo("room_id")
    val roomId: RoomId? = null,
    val type: String,
    @ColumnInfo("origin_server_ts")
    val originServerTs: Long,
    @ColumnInfo("state_key")
    val stateKey: String,
    val unsigned: UnsignedRoomEventData.UnsignedStateEventData? = null,

    @PrimaryKey
    val id: String = eventId.full
) {

    suspend inline fun <reified C: EventContent> asClientEvent(json: Json): ClientEvent<C> {
        return withContext(Dispatchers.Default) {
            json.decodeFromString(
                string = json.encodeToString(this)
            )
        }
    }
}