package data.io.matrix.room.event.serialization

import data.io.matrix.room.event.content.MatrixClientEvent.RoomEvent
import data.io.matrix.room.event.content.MatrixClientEvent.RoomEvent.MessageEvent
import data.io.matrix.room.event.content.MatrixClientEvent.RoomEvent.StateEvent
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject
import net.folivo.trixnity.core.serialization.canonicalJson

class RoomEventSerializer(
    private val messageEventSerializer: KSerializer<MessageEvent<*>>,
    private val stateEventSerializer: KSerializer<StateEvent<*>>,
) : KSerializer<RoomEvent<*>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("RoomEventSerializer")

    override fun deserialize(decoder: Decoder): RoomEvent<*> {
        require(decoder is JsonDecoder)
        val jsonObj = decoder.decodeJsonElement().jsonObject
        val hasStateKey = "state_key" in jsonObj
        val serializer = if (hasStateKey) stateEventSerializer else messageEventSerializer
        return decoder.json.decodeFromJsonElement(serializer, jsonObj)
    }

    override fun serialize(encoder: Encoder, value: RoomEvent<*>) {
        require(encoder is JsonEncoder)
        val jsonElement = when (value) {
            is MessageEvent -> encoder.json.encodeToJsonElement(messageEventSerializer, value)
            is StateEvent -> encoder.json.encodeToJsonElement(stateEventSerializer, value)
        }
        encoder.encodeJsonElement(canonicalJson(jsonElement))
    }
}