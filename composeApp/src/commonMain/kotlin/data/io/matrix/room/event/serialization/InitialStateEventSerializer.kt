package data.io.matrix.room.event.serialization

import data.io.matrix.room.event.content.InitialStateEvent
import data.io.matrix.room.event.content.StateEventContent


class InitialStateEventSerializer(
    stateEventContentSerializers: Set<EventContentSerializerMapping<StateEventContent>>,
) : BaseEventSerializer<StateEventContent, InitialStateEvent<*>>(
    "InitialStateEvent",
    RoomEventContentToEventSerializerMappings(
        baseMapping = stateEventContentSerializers,
        eventDeserializer = { InitialStateEvent.serializer(it.serializer) },
        unknownEventSerializer = { InitialStateEvent.serializer(UnknownEventContentSerializer(it)) },
        redactedEventSerializer = { InitialStateEvent.serializer(RedactedEventContentSerializer(it)) },
    )
)