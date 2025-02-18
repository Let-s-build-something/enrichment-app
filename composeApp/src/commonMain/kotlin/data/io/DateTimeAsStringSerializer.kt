package data.io

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor

object DateTimeAsStringSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): LocalDateTime {
        return LocalDateTime.parse(
            decoder.decodeString(),
            LocalDateTime.Formats.ISO
        )
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(LocalDateTime.Formats.ISO))
    }
}

object InstantAsStringSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Instant {
        return Instant.parse(
            decoder.decodeString(),
            format = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
        )
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Instant) {
        encoder.encodeString(
            value.format(format = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET)
        )
    }
}
