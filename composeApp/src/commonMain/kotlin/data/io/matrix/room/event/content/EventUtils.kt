package data.io.matrix.room.event.content

import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId

val MatrixClientEvent<*>.stateKeyOrNull: String?
    get() = when (this) {
        is MatrixClientEvent.StateBaseEvent -> this.stateKey
        else -> null
    }

val MatrixClientEvent<*>.idOrNull: EventId?
    get() = when (this) {
        is MatrixClientEvent.RoomEvent -> this.id
        else -> null
    }

val MatrixClientEvent<*>.originTimestampOrNull: Long?
    get() = when (this) {
        is MatrixClientEvent.RoomEvent -> this.originTimestamp
        else -> null
    }

val MatrixClientEvent<*>.roomIdOrNull: RoomId?
    get() = when (this) {
        is MatrixClientEvent.RoomEvent -> this.roomId
        is MatrixClientEvent.StrippedStateEvent -> this.roomId
        is MatrixClientEvent.RoomAccountDataEvent -> this.roomId
        is MatrixClientEvent.EphemeralEvent -> this.roomId
        else -> null
    }

val MatrixClientEvent<*>.senderOrNull: UserId?
    get() = when (this) {
        is MatrixClientEvent.RoomEvent -> this.sender
        is MatrixClientEvent.StrippedStateEvent -> this.sender
        is MatrixClientEvent.ToDeviceEvent -> this.sender
        is MatrixClientEvent.EphemeralEvent -> this.sender
        else -> null
    }