package data.io.matrix

import data.io.matrix.room.RoomsResponseIO
import kotlinx.serialization.Serializable
import net.folivo.trixnity.clientserverapi.model.sync.OneTimeKeysCount
import net.folivo.trixnity.clientserverapi.model.sync.Sync.Response.DeviceLists
import net.folivo.trixnity.clientserverapi.model.sync.Sync.Response.GlobalAccountData
import net.folivo.trixnity.clientserverapi.model.sync.Sync.Response.Presence
import net.folivo.trixnity.clientserverapi.model.sync.Sync.Response.ToDevice
import net.folivo.trixnity.clientserverapi.model.sync.UnusedFallbackKeyTypes

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
    val accountData: GlobalAccountData? = null,

    /** Required: The batch token to supply in the since param of the next /sync request. */
    val nextBatch: String? = null,

    /** Updates to rooms. */
    val rooms: RoomsResponseIO? = null,

    /** The updates to the presence status of other users. */
    val presence: Presence? = null,

    val toDevice: ToDevice? = null,
    val deviceLists: DeviceLists? = null,
    val oneTimeKeysCount: OneTimeKeysCount? = null,
    val unusedFallbackKeyTypes: UnusedFallbackKeyTypes? = null,
)
