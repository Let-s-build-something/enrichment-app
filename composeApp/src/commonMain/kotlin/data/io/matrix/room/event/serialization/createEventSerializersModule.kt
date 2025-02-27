package data.io.matrix.room.event.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.folivo.trixnity.core.model.events.EventContent
import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.StateEventContent
import net.folivo.trixnity.core.model.events.m.PresenceEventContent
import net.folivo.trixnity.core.model.events.m.ReactionEventContent
import net.folivo.trixnity.core.model.events.m.ReceiptEventContent
import net.folivo.trixnity.core.model.events.m.TypingEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.SasAcceptEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.SasKeyEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.SasMacEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationDoneEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationReadyEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationStartEventContent
import net.folivo.trixnity.core.model.events.m.policy.RoomRuleEventContent
import net.folivo.trixnity.core.model.events.m.policy.ServerRuleEventContent
import net.folivo.trixnity.core.model.events.m.policy.UserRuleEventContent
import net.folivo.trixnity.core.model.events.m.room.AvatarEventContent
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContentSerializer
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import net.folivo.trixnity.core.model.events.m.room.GuestAccessEventContent
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import net.folivo.trixnity.core.model.events.m.room.PinnedEventsEventContent
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent
import net.folivo.trixnity.core.model.events.m.room.RedactionEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContentSerializer
import net.folivo.trixnity.core.model.events.m.room.ServerACLEventContent
import net.folivo.trixnity.core.model.events.m.room.ThirdPartyInviteEventContent
import net.folivo.trixnity.core.model.events.m.room.TombstoneEventContent
import net.folivo.trixnity.core.model.events.m.room.TopicEventContent
import net.folivo.trixnity.core.model.events.m.space.ChildEventContent
import net.folivo.trixnity.core.model.events.m.space.ParentEventContent
import net.folivo.trixnity.core.serialization.canonicalJson
import net.folivo.trixnity.core.serialization.events.DecryptedMegolmEventSerializer
import net.folivo.trixnity.core.serialization.events.DecryptedOlmEventSerializer
import net.folivo.trixnity.core.serialization.events.EphemeralEventSerializer
import net.folivo.trixnity.core.serialization.events.EventContentSerializerMapping
import net.folivo.trixnity.core.serialization.events.EventContentSerializerMappings
import net.folivo.trixnity.core.serialization.events.EventTypeSerializer
import net.folivo.trixnity.core.serialization.events.GlobalAccountDataEventSerializer
import net.folivo.trixnity.core.serialization.events.InitialStateEventSerializer
import net.folivo.trixnity.core.serialization.events.MessageEventContentSerializerMapping
import net.folivo.trixnity.core.serialization.events.MessageEventSerializer
import net.folivo.trixnity.core.serialization.events.RoomAccountDataEventSerializer
import net.folivo.trixnity.core.serialization.events.RoomEventSerializer
import net.folivo.trixnity.core.serialization.events.StateBaseEventSerializer
import net.folivo.trixnity.core.serialization.events.StateEventContentSerializerMapping
import net.folivo.trixnity.core.serialization.events.StateEventSerializer
import net.folivo.trixnity.core.serialization.events.StrippedStateEventSerializer
import net.folivo.trixnity.core.serialization.events.ToDeviceEventSerializer
import net.folivo.trixnity.core.serialization.events.contentSerializer

fun createEventSerializersModule(
    mappings: EventContentSerializerMappings,
): SerializersModule {
    val contextualMessageEventContentSerializer = ContextualMessageEventContentSerializer(mappings.message)
    val contextualStateEventContentSerializer = ContextualStateEventContentSerializer(mappings.state)

    // hacked serialization strategies
    val messageEventSerializer = RoomIdInjectingSerializer(MessageEventSerializer(mappings.message))
    val stateEventSerializer = RoomIdInjectingSerializer(StateEventSerializer(mappings.state))
    val roomEventSerializer = RoomIdInjectingSerializer(RoomEventSerializer(messageEventSerializer, stateEventSerializer))
    val strippedStateEventSerializer = RoomIdInjectingSerializer(StrippedStateEventSerializer(mappings.state))
    val stateBaseEventSerializer = RoomIdInjectingSerializer(StateBaseEventSerializer(stateEventSerializer, strippedStateEventSerializer))
    val initialStateEventSerializer = RoomIdInjectingSerializer(InitialStateEventSerializer(mappings.state))
    val ephemeralEventSerializer = RoomIdInjectingSerializer(EphemeralEventSerializer(mappings.ephemeral))
    val toDeviceEventSerializer = RoomIdInjectingSerializer(ToDeviceEventSerializer(mappings.toDevice))
    val decryptedOlmEventSerializer =
        RoomIdInjectingSerializer(DecryptedOlmEventSerializer(
            @Suppress("UNCHECKED_CAST")
            ((mappings.message + mappings.state + mappings.ephemeral + mappings.toDevice) as Set<EventContentSerializerMapping<EventContent>>)
        ))
    val decryptedMegolmEventSerializer = RoomIdInjectingSerializer(DecryptedMegolmEventSerializer(mappings.message))
    val globalAccountDataEventSerializer = RoomIdInjectingSerializer(GlobalAccountDataEventSerializer(mappings.globalAccountData))
    val roomAccountDataEventSerializer = RoomIdInjectingSerializer(RoomAccountDataEventSerializer(mappings.roomAccountData))

    val eventTypeSerializer = EventTypeSerializer(mappings)
    return SerializersModule {
        contextual(contextualMessageEventContentSerializer)
        contextual(contextualStateEventContentSerializer)
        contextual(roomEventSerializer)
        contextual(messageEventSerializer)
        contextual(stateEventSerializer)
        contextual(strippedStateEventSerializer)
        contextual(stateBaseEventSerializer)
        contextual(initialStateEventSerializer)
        contextual(ephemeralEventSerializer)
        contextual(toDeviceEventSerializer)
        contextual(decryptedOlmEventSerializer)
        contextual(decryptedMegolmEventSerializer)
        contextual(globalAccountDataEventSerializer)
        contextual(roomAccountDataEventSerializer)
        contextual(eventTypeSerializer)
    }
}

internal val DefaultLocalSerializerMappings = createEventSerializersModule(
    createEventContentSerializerMappings {
        messageOf("m.room.message", RoomMessageEventContentSerializer)
        messageOf<ReactionEventContent>("m.reaction")
        messageOf<RedactionEventContent>("m.room.redaction")
        messageOf("m.room.encrypted", EncryptedMessageEventContentSerializer)
        messageOf<VerificationStartEventContent>("m.key.verification.start")
        messageOf<VerificationReadyEventContent>("m.key.verification.ready")
        messageOf<VerificationDoneEventContent>("m.key.verification.done")
        messageOf<VerificationCancelEventContent>("m.key.verification.cancel")
        messageOf<SasAcceptEventContent>("m.key.verification.accept")
        messageOf<SasKeyEventContent>("m.key.verification.key")
        messageOf<SasMacEventContent>("m.key.verification.mac")

        stateOf<AvatarEventContent>("m.room.avatar")
        stateOf<CanonicalAliasEventContent>("m.room.canonical_alias")
        stateOf<CreateEventContent>("m.room.create")
        stateOf<JoinRulesEventContent>("m.room.join_rules")
        stateOf<MemberEventContent>("m.room.member")
        stateOf<NameEventContent>("m.room.name")
        stateOf<PinnedEventsEventContent>("m.room.pinned_events")
        stateOf<PowerLevelsEventContent>("m.room.power_levels")
        stateOf<TopicEventContent>("m.room.topic")
        stateOf<EncryptionEventContent>("m.room.encryption")
        stateOf<HistoryVisibilityEventContent>("m.room.history_visibility")
        stateOf<ThirdPartyInviteEventContent>("m.room.third_party_invite")
        stateOf<GuestAccessEventContent>("m.room.guest_access")
        stateOf<ServerACLEventContent>("m.room.server_acl")
        stateOf<TombstoneEventContent>("m.room.tombstone")
        stateOf<UserRuleEventContent>("m.policy.rule.user")
        stateOf<RoomRuleEventContent>("m.policy.rule.room")
        stateOf<ServerRuleEventContent>("m.policy.rule.server")
        stateOf<ParentEventContent>("m.space.parent")
        stateOf<ChildEventContent>("m.space.child")

        ephemeralOf<PresenceEventContent>("m.presence")
        ephemeralOf<TypingEventContent>("m.typing")
        ephemeralOf<ReceiptEventContent>("m.receipt")
    }
)

internal class ContextualStateEventContentSerializer(
    private val mappings: Set<StateEventContentSerializerMapping>,
) : KSerializer<StateEventContent> {
    override val descriptor = buildClassSerialDescriptor("ContextualStateEventContentSerializer")

    override fun deserialize(decoder: Decoder): StateEventContent {
        require(decoder is JsonDecoder)
        val jsonObject = decoder.decodeJsonElement().jsonObject
        val type = (jsonObject["type"] as? JsonPrimitive)?.content // this is a fallback (e.g. for unsigned)
            ?: throw SerializationException("type must not be null for deserializing StateEventContent")

        val serializer = mappings.contentSerializer(type)

        return decoder.json.decodeFromJsonElement(serializer, jsonObject)
    }

    override fun serialize(encoder: Encoder, value: StateEventContent) {
        require(encoder is JsonEncoder)
        val serializer = mappings.contentSerializer(value)
        encoder.encodeJsonElement(canonicalJson(encoder.json.encodeToJsonElement(serializer, value)))
    }
}

internal class ContextualMessageEventContentSerializer(
    private val mappings: Set<MessageEventContentSerializerMapping>,
) : KSerializer<MessageEventContent> {
    override val descriptor = buildClassSerialDescriptor("ContextualMessageEventContentSerializer")

    override fun deserialize(decoder: Decoder): MessageEventContent {
        require(decoder is JsonDecoder)
        val jsonObject = decoder.decodeJsonElement().jsonObject
        val type = (jsonObject["type"] as? JsonPrimitive)?.content // this is a fallback (e.g. for RelatesTo)
            ?: throw SerializationException("type must not be null for deserializing MessageEventContent")

        val serializer = mappings.contentSerializer(type)
        return decoder.json.decodeFromJsonElement(serializer, jsonObject)
    }

    override fun serialize(encoder: Encoder, value: MessageEventContent) {
        require(encoder is JsonEncoder)
        val serializer = mappings.contentSerializer(value)
        encoder.encodeJsonElement(canonicalJson(encoder.json.encodeToJsonElement(serializer, value)))
    }
}
