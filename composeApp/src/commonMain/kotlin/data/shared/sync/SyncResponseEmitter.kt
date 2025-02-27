package data.shared.sync

import data.io.matrix.SyncResponse
import net.folivo.trixnity.core.ClientEventEmitterImpl
import net.folivo.trixnity.core.model.events.ClientEvent

class SyncEvents(
    val syncResponse: SyncResponse,
    allEvents: List<ClientEvent<*>>,
) : List<ClientEvent<*>> by allEvents

class SyncResponseEmitter: ClientEventEmitterImpl<SyncEvents>() {

}