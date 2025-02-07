package data.io.matrix

import data.io.matrix.room.RoomAccountData
import data.io.matrix.room.RoomsResponseIO
import kotlinx.serialization.Serializable

/**
 * Synchronise the client’s state with the latest state on the server.
 * Clients use this API when they first log in to get an initial snapshot of the state on the server,
 * and then continue to call this API to get incremental deltas to the state, and to receive new messages.
 *
 * https://spec.matrix.org/v1.13/client-server-api/#get_matrixclientv3sync
 */
@Serializable
data class SyncResponse(
    /** The global private data created by this user. */
    val accountData: RoomAccountData? = null,

    /** Required: The batch token to supply in the since param of the next /sync request. */
    val nextBatch: String? = null,

    /** Updates to rooms. */
    val rooms: RoomsResponseIO? = null,

    /** The updates to the presence status of other users. */
    val presence: RoomAccountData? = null
)
