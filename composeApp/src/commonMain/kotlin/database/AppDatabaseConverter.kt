package database

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
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

    /** Converts list of strings to string */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return if(value.isNullOrEmpty()) null else value.joinToString(",")
    }

    /** Converts string to list of strings */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")
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
