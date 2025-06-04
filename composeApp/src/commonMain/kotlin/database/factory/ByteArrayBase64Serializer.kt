package database.factory

import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class ByteArrayBase64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ByteArrayBase64Serializer")

    override fun deserialize(decoder: Decoder): ByteArray {
        return decoder.decodeString().decodeBase64Bytes()
    }

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(value.encodeBase64())
    }
}