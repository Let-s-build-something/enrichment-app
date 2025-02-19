package data.io.matrix.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import database.AppRoomDatabase.Companion.TABLE_ROOM_EVENT
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Entity(tableName = TABLE_ROOM_EVENT)
@Serializable
open class MatrixEvent @OptIn(ExperimentalUuidApi::class) constructor(
    /** Required: The fields in this object will vary depending on the type of event. */
    val content: MatrixEventContent? = null,

    /** Required: The type of event. This SHOULD be namespaced similar to Java package naming conventions e.g. ‘com.example.subdomain.event.type’ */
    val type: String? = null,

    /** Required: The state_key for the event. */
    @ColumnInfo("state_key")
    val stateKey: String? = null,

    /** Required: Contains the fully-qualified ID of the user who sent this event. */
    val sender: String? = null,

    /** Required: The globally unique event identifier. */
    @ColumnInfo("event_id")
    val eventId: String? = null,

    /** Required: The time the event was sent to the server as a unix timestamp in milliseconds. */
    @ColumnInfo("origin_server_ts")
    val originServerTs: Long? = null,

    /** Present if, and only if, this event is a state event.
     *  The key making this piece of state unique in the room. */
    val state: String? = null,

    /** Contains optional extra information about the event. */
    val unsigned: UnsignedRoomEventData? = null,

    @ColumnInfo("room_id")
    val roomId: String? = null,

    @PrimaryKey
    val id: String = eventId ?: Uuid.random().toString()
)
