package koin

import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import coil3.annotation.ExperimentalCoilApi
import coil3.network.NetworkFetcher
import coil3.network.ktor3.asNetworkClient
import data.io.matrix.room.event.content.AvatarEventContent
import data.io.matrix.room.event.content.CanonicalAliasEventContent
import data.io.matrix.room.event.content.CreateEventContent
import data.io.matrix.room.event.content.EncryptionEventContent
import data.io.matrix.room.event.content.GuestAccessEventContent
import data.io.matrix.room.event.content.HistoryVisibilityEventContent
import data.io.matrix.room.event.content.JoinRulesEventContent
import data.io.matrix.room.event.content.MemberEventContent
import data.io.matrix.room.event.content.NameEventContent
import data.io.matrix.room.event.content.PinnedEventsEventContent
import data.io.matrix.room.event.content.PowerLevelsEventContent
import data.io.matrix.room.event.content.PresenceEventContent
import data.io.matrix.room.event.content.ReactionEventContent
import data.io.matrix.room.event.content.ReceiptEventContent
import data.io.matrix.room.event.content.RedactionEventContent
import data.io.matrix.room.event.content.RoomMessageEventContentSerializer
import data.io.matrix.room.event.content.ServerACLEventContent
import data.io.matrix.room.event.content.ThirdPartyInviteEventContent
import data.io.matrix.room.event.content.TombstoneEventContent
import data.io.matrix.room.event.content.TopicEventContent
import data.io.matrix.room.event.content.TypingEventContent
import data.io.matrix.room.event.content.key.SasAcceptEventContent
import data.io.matrix.room.event.content.key.SasKeyEventContent
import data.io.matrix.room.event.content.key.SasMacEventContent
import data.io.matrix.room.event.content.key.VerificationCancelEventContent
import data.io.matrix.room.event.content.key.VerificationDoneEventContent
import data.io.matrix.room.event.content.key.VerificationReadyEventContent
import data.io.matrix.room.event.content.key.VerificationStartEventContent
import data.io.matrix.room.event.content.rule.RoomRuleEventContent
import data.io.matrix.room.event.content.rule.ServerRuleEventContent
import data.io.matrix.room.event.content.rule.UserRuleEventContent
import data.io.matrix.room.event.content.space.ChildEventContent
import data.io.matrix.room.event.content.space.ParentEventContent
import data.io.matrix.room.event.serialization.EncryptedMessageEventContentSerializer
import data.io.matrix.room.event.serialization.createEventContentSerializerMappings
import data.io.matrix.room.event.serialization.createEventSerializersModule
import data.io.matrix.room.event.serialization.ephemeralOf
import data.io.matrix.room.event.serialization.messageOf
import data.io.matrix.room.event.serialization.stateOf
import data.shared.DeveloperConsoleViewModel
import data.shared.SharedDataManager
import data.shared.SharedRepository
import data.shared.SharedViewModel
import data.shared.appServiceModule
import data.shared.auth.authModule
import data.shared.developerConsoleModule
import data.shared.sync.dataSyncModule
import database.databaseModule
import database.file.FileAccess
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.ktor.client.HttpClient
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.overwriteWith
import net.folivo.trixnity.core.serialization.events.DefaultEventContentSerializerMappings
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.conversation.components.audio.mediaProcessorModule
import ui.home.homeModule

/** Common module for the whole application */
@OptIn(ExperimentalCoilApi::class, ExperimentalSerializationApi::class)
internal val commonModule = module {
    if(currentPlatform != PlatformType.Jvm) includes(settingsModule)
    single { FileAccess() }
    single { SharedDataManager() }
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            useArrayPolymorphism = true
            coerceInputValues = true
            encodeDefaults = true
            explicitNulls = false
            allowSpecialFloatingPointValues = true
            allowStructuredMapKeys = true
            prettyPrint = true
            namingStrategy = JsonNamingStrategy.SnakeCase
            serializersModule = net.folivo.trixnity.core.serialization.events.createEventSerializersModule(
                DefaultEventContentSerializerMappings
            ).overwriteWith(DefaultLocalSerializerMappings)
        }
    }

    includes(databaseModule)
    includes(dataSyncModule)
    includes(authModule)
    viewModelOf(::SharedViewModel)
    includes(homeModule)
    includes(appServiceModule)
    includes(mediaProcessorModule)

    single {
        NetworkFetcher.Factory(
            networkClient = { get<HttpClient>().asNetworkClient() }
        )
    }

    val isDev = try {
        Firebase.auth.currentUser?.email?.endsWith("@augmy.org") == true
    }catch (e: NotImplementedError) {
        true // enabled on all JVM devices for now as there is no email getter
    }.also {
        if(it) this@module.includes(developerConsoleModule)
    }

    single {
        httpClientFactory(
            sharedViewModel = get<SharedViewModel>(),
            developerViewModel = if(isDev) get<DeveloperConsoleViewModel>() else null,
            json = get<Json>()
        )
    }

    factory { SharedRepository(get<HttpClient>()) }
}

private val DefaultLocalSerializerMappings = createEventSerializersModule(
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

expect val settings: AppSettings

expect val secureSettings: SecureAppSettings
