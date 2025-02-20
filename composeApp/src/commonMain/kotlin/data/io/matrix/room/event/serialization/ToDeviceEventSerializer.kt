package data.io.matrix.room.event.serialization

import data.io.matrix.room.event.content.MatrixClientEvent.ToDeviceEvent
import data.io.matrix.room.event.content.ToDeviceEventContent


class ToDeviceEventSerializer(
    toDeviceEventContentSerializers: Set<EventContentSerializerMapping<ToDeviceEventContent>>,
) : BaseEventSerializer<ToDeviceEventContent, ToDeviceEvent<*>>(
    "ToDeviceEvent",
    EventContentToEventSerializerMappings(
        baseMapping = toDeviceEventContentSerializers,
        eventDeserializer = { ToDeviceEvent.serializer(it.serializer) },
        unknownEventSerializer = { ToDeviceEvent.serializer(UnknownEventContentSerializer(it)) },
    )
)