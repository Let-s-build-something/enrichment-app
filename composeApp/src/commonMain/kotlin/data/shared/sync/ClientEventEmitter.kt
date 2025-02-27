package data.shared.sync

import net.folivo.trixnity.core.ClientEventEmitterImpl
import net.folivo.trixnity.core.model.events.ClientEvent

class ClientEventEmitter: ClientEventEmitterImpl<List<ClientEvent<*>>>() {

}