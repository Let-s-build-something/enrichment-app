package data.io.matrix.room.event.serialization

import data.io.matrix.room.event.content.MatrixClientEvent.StrippedStateEvent
import data.io.matrix.room.event.content.StateEventContent


class StrippedStateEventSerializer(
    stateEventContentSerializers: Set<EventContentSerializerMapping<StateEventContent>>,
) : BaseEventSerializer<StateEventContent, StrippedStateEvent<*>>(
    "StrippedStateEvent",
    RoomEventContentToEventSerializerMappings(
        baseMapping = stateEventContentSerializers,
        eventDeserializer = { PutTypeIntoPrevContentSerializer(StrippedStateEvent.serializer(it.serializer)) },
        unknownEventSerializer = {
            PutTypeIntoPrevContentSerializer(StrippedStateEvent.serializer(UnknownEventContentSerializer(it)))
        },
        redactedEventSerializer = {
            PutTypeIntoPrevContentSerializer(StrippedStateEvent.serializer(RedactedEventContentSerializer(it)))
        },
    )
)