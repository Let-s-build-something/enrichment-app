package data.io.matrix.room.event.serialization

import data.io.matrix.room.event.content.MatrixClientEvent.RoomEvent.StateEvent
import data.io.matrix.room.event.content.StateEventContent


class StateEventSerializer(
    stateEventContentSerializers: Set<EventContentSerializerMapping<StateEventContent>>,
) : BaseEventSerializer<StateEventContent, StateEvent<*>>(
    "StateEvent",
    RoomEventContentToEventSerializerMappings(
        baseMapping = stateEventContentSerializers,
        eventDeserializer = { PutTypeIntoPrevContentSerializer(StateEvent.serializer(it.serializer)) },
        unknownEventSerializer = {
            PutTypeIntoPrevContentSerializer(StateEvent.serializer(UnknownEventContentSerializer(it)))
        },
        redactedEventSerializer = {
            PutTypeIntoPrevContentSerializer(StateEvent.serializer(RedactedEventContentSerializer(it)))
        },
    )
)