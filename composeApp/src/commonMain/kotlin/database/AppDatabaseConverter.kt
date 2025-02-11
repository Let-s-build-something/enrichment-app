package database

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import data.io.matrix.room.RoomAccountData
import data.io.matrix.room.RoomEphemeral
import data.io.matrix.room.RoomInviteState
import data.io.matrix.room.RoomNotificationsCount
import data.io.matrix.room.RoomSummary
import data.io.matrix.room.RoomTimeline
import data.io.social.network.conversation.giphy.GifAsset
import data.io.social.network.conversation.message.ConversationAnchorMessageIO
import data.io.social.network.conversation.message.MediaIO
import data.io.social.network.conversation.message.MessageReactionIO
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

    // empty converters
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
