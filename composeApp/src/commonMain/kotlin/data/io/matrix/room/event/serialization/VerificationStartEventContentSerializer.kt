package data.io.matrix.room.event.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.key.verification.SasHash
import net.folivo.trixnity.core.model.events.m.key.verification.SasKeyAgreementProtocol
import net.folivo.trixnity.core.model.events.m.key.verification.SasMessageAuthenticationCode
import net.folivo.trixnity.core.model.events.m.key.verification.SasMethod
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationStartEventContent

object VerificationStartEventContentSerializer : KSerializer<VerificationStartEventContent> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("VerificationStartEventContent") {
            element<String>("method")
            element<String>("from_device")
            element<Set<SasHash>>("hashes", isOptional = true)
            element<Set<SasKeyAgreementProtocol>>("key_agreement_protocols", isOptional = true)
            element<Set<SasMessageAuthenticationCode>>("message_authentication_codes", isOptional = true)
            element<Set<SasMethod>>("short_authentication_string", isOptional = true)
            element<RelatesTo.Reference>("m.relates_to", isOptional = true)
            element<String>("transaction_id", isOptional = true)
            element<String>("room_id", isOptional = true)
        }

    override fun serialize(encoder: Encoder, value: VerificationStartEventContent) {
        require(encoder is JsonEncoder) { "This serializer requires a JsonEncoder" }
        when (value) {
            is VerificationStartEventContent.SasStartEventContent -> {
                val jsonObject = buildJsonObject {
                    put("method", "m.sas.v1")
                    put("from_device", value.fromDevice)
                    put("hashes", encoder.json.encodeToJsonElement(value.hashes))
                    put("key_agreement_protocols", encoder.json.encodeToJsonElement(value.keyAgreementProtocols))
                    put("message_authentication_codes", encoder.json.encodeToJsonElement(value.messageAuthenticationCodes))
                    put("short_authentication_string", encoder.json.encodeToJsonElement(value.shortAuthenticationString))
                    value.relatesTo?.let {
                        put("m.relates_to", encoder.json.encodeToJsonElement(it))
                    }
                    value.transactionId?.let {
                        put("transaction_id", it)
                    }
                }
                encoder.encodeJsonElement(jsonObject)
            }
        }
    }

    override fun deserialize(decoder: Decoder): VerificationStartEventContent {
        require(decoder is JsonDecoder) { "This serializer requires a JsonDecoder" }
        val jsonElement = decoder.decodeJsonElement()

        if (jsonElement !is JsonObject) {
            throw SerializationException("Expected JsonObject for VerificationStartEventContent, got ${jsonElement::class.simpleName}")
        }
        val updatedJson = if ("room_id" in jsonElement) {
            jsonElement
        } else {
            JsonObject(jsonElement.toMutableMap().apply {
                put("room_id", JsonPrimitive(""))
            })
        }
        val method = updatedJson["method"]?.jsonPrimitive?.content
            ?: throw SerializationException("Missing 'method' field in VerificationStartEventContent")

        return when (method) {
            "m.sas.v1" -> decoder.json.decodeFromJsonElement<VerificationStartEventContent.SasStartEventContent>(updatedJson)
            else -> throw SerializationException("Unknown method: $method")
        }
    }
}