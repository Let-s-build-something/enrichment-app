package data.io.matrix.room.event.serialization

import data.io.matrix.room.event.content.DecryptedMegolmEvent
import data.io.matrix.room.event.content.MessageEventContent


class DecryptedMegolmEventSerializer(
    messageEventContentSerializers: Set<EventContentSerializerMapping<MessageEventContent>>,
) : BaseEventSerializer<MessageEventContent, DecryptedMegolmEvent<*>>(
    "DecryptedMegolmEvent",
    RoomEventContentToEventSerializerMappings(
        baseMapping = messageEventContentSerializers,
        eventDeserializer = { DecryptedMegolmEvent.serializer(it.serializer) },
        unknownEventSerializer = { DecryptedMegolmEvent.serializer(UnknownEventContentSerializer(it)) },
        redactedEventSerializer = { DecryptedMegolmEvent.serializer(RedactedEventContentSerializer(it)) },
    )
)