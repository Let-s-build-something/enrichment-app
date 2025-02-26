package database

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import data.io.matrix.room.RoomAccountData
import data.io.matrix.room.RoomEphemeral
import data.io.matrix.room.RoomInviteState
import data.io.matrix.room.RoomNotificationsCount
import data.io.matrix.room.RoomSummary
import data.io.matrix.room.RoomTimeline
import data.io.matrix.room.event.content.PresenceEventContent
import data.io.social.network.conversation.giphy.GifAsset
import data.io.social.network.conversation.message.ConversationAnchorMessageIO
import data.io.social.network.conversation.message.MediaIO
import data.io.social.network.conversation.message.MessageReactionIO
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.json.Json
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.EventIdSerializer
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.RoomIdSerializer
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import net.folivo.trixnity.core.model.keys.Curve25519KeySerializer
import net.folivo.trixnity.core.model.keys.Ed25519KeySerializer
import net.folivo.trixnity.core.model.keys.Key
import org.koin.mp.KoinPlatform

/** Factory converter for Room database */
@ProvidedTypeConverter
class AppDatabaseConverter {

    private val json: Json by KoinPlatform.getKoin().inject()

    /** Converts long to a string */
    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        return value?.joinToString(",")
    }

    /** Converts string to an integer long */
    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        return value?.split(",")?.mapNotNull { it.toLongOrNull() }
    }

    /** Converts object to string */
    @TypeConverter
    fun fromRoomSummary(value: RoomSummary): String {
        return json.encodeToString(value)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toRoomSummary(value: String): RoomSummary {
        return json.decodeFromString(value)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromPresenceEventContent(value: PresenceEventContent): String {
        return json.encodeToString(value)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toPresenceEventContent(value: String): PresenceEventContent {
        return json.decodeFromString(value)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromRoomId(value: RoomId): String {
        return json.encodeToString(value = value, serializer = RoomIdSerializer)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toRoomId(value: String): RoomId {
        return json.decodeFromString(string = value, deserializer = RoomIdSerializer)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromInstant(value: Instant): String {
        return json.encodeToString(value = value, serializer = InstantIso8601Serializer)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toInstant(value: String): Instant {
        return json.decodeFromString(string = value, deserializer = InstantIso8601Serializer)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromCurve25519(value: Key.Curve25519Key): String {
        return json.encodeToString(value = value, serializer = Curve25519KeySerializer)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toCurve25519(value: String): Key.Curve25519Key {
        return json.decodeFromString(string = value, deserializer = Curve25519KeySerializer)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromCurve25519List(value: List<Key.Curve25519Key>): String {
        return json.encodeToString(value = value)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toCurve25519List(value: String): List<Key.Curve25519Key> {
        return json.decodeFromString(string = value)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromEd25519(value: Key.Ed25519Key): String {
        return json.encodeToString(value = value, serializer = Ed25519KeySerializer)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toEd25519(value: String): Key.Ed25519Key {
        return json.decodeFromString(string = value, deserializer = Ed25519KeySerializer)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromEventId(value: EventId): String {
        return json.encodeToString(value = value, serializer = EventIdSerializer)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toEventId(value: String): EventId {
        return json.decodeFromString(string = value, deserializer = EventIdSerializer)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromNewDevices(value: Map<UserId, Set<String>>): String {
        return json.encodeToString(value)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toNewDevices(value: String): Map<UserId, Set<String>> {
        return json.decodeFromString(value)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromUnsignedStateEventData(value: UnsignedRoomEventData.UnsignedStateEventData): String {
        return json.encodeToString(value)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toUnsignedStateEventData(value: String): UnsignedRoomEventData.UnsignedStateEventData {
        return json.decodeFromString(value)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromRoomNotificationsCount(value: RoomNotificationsCount): String {
        return json.encodeToString(value)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toRoomNotificationsCount(value: String): RoomNotificationsCount {
        return json.decodeFromString(value)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromRoomEphemeral(value: RoomEphemeral): String {
        return json.encodeToString(value)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toRoomEphemeral(value: String): RoomEphemeral {
        return json.decodeFromString(value)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromRoomAccountData(value: RoomAccountData): String {
        return json.encodeToString(value)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toRoomAccountData(value: String): RoomAccountData {
        return json.decodeFromString(value)
    }

    // empty converters, we don't want it saved, yet we can't ignore it
    @TypeConverter
    fun fromRoomTimeline(value: RoomTimeline): String? {
        return null
    }
    @TypeConverter
    fun toRoomTimeline(value: String): RoomTimeline? {
        return null
    }

    /** Converts object to string */
    @TypeConverter
    fun fromRoomInviteState(value: RoomInviteState): String {
        return json.encodeToString(value)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toRoomInviteState(value: String): RoomInviteState {
        return json.decodeFromString(value)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromUserId(value: UserId): String {
        return json.encodeToString(value)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toUserId(value: String): UserId {
        return json.decodeFromString(value)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromMediaIO(value: MediaIO?): String? {
        return if(value == null) null else json.encodeToString(value)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toMediaIO(value: String?): MediaIO? {
        return if(value == null) null else json.decodeFromString(value)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromGifAsset(value: GifAsset): String {
        return json.encodeToString(value)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toGifAsset(value: String): GifAsset {
        return json.decodeFromString(value)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromMediaList(value: List<MediaIO>?): String? {
        return if(value.isNullOrEmpty()) null else json.encodeToString(value)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toMediaList(value: String?): List<MediaIO>? {
        return if(value == null) null else json.decodeFromString(value)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromReactionList(value: List<MessageReactionIO>?): String? {
        return if(value.isNullOrEmpty()) null else json.encodeToString(value)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toReactionList(value: String?): List<MessageReactionIO>? {
        return if(value == null) null else json.decodeFromString(value)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime): String {
        return value.format(LocalDateTime.Formats.ISO)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toLocalDateTime(value: String): LocalDateTime {
        return LocalDateTime.parse(value, LocalDateTime.Formats.ISO)
    }

    /** Converts object to string */
    @TypeConverter
    fun fromConversationAnchorMessageIO(value: ConversationAnchorMessageIO): String {
        return json.encodeToString(value)
    }

    /** Converts string to an object */
    @TypeConverter
    fun toConversationAnchorMessageIO(value: String): ConversationAnchorMessageIO? {
        return json.decodeFromString(value)
    }
}
