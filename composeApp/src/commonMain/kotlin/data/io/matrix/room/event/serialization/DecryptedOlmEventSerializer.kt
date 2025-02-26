package data.io.matrix.room.event.serialization

import data.io.matrix.room.event.content.EventContent


class DecryptedOlmEventSerializer(
    eventContentSerializers: Set<EventContentSerializerMapping<EventContent>>,
) : BaseEventSerializer<EventContent, DecryptedOlmEvent<*>>(
    "DecryptedOlmEvent",
    EventContentToEventSerializerMappings(
        baseMapping = eventContentSerializers,
        eventDeserializer = { DecryptedOlmEvent.serializer(it.serializer) },
        unknownEventSerializer = { DecryptedOlmEvent.serializer(UnknownEventContentSerializer(it)) },
    )
)