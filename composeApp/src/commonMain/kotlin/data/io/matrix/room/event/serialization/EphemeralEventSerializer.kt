package data.io.matrix.room.event.serialization

import data.io.matrix.room.event.content.EphemeralEventContent
import data.io.matrix.room.event.content.MatrixClientEvent.EphemeralEvent


class EphemeralEventSerializer(
    ephemeralEventContentSerializers: Set<EventContentSerializerMapping<EphemeralEventContent>>,
) : BaseEventSerializer<EphemeralEventContent, EphemeralEvent<*>>(
    "EphemeralEvent",
    EventContentToEventSerializerMappings(
        baseMapping = ephemeralEventContentSerializers,
        eventDeserializer = { EphemeralEvent.serializer(it.serializer) },
        unknownEventSerializer = { EphemeralEvent.serializer(UnknownEventContentSerializer(it)) },
    )
)