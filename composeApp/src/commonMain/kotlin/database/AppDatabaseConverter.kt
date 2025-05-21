package database

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import data.io.matrix.room.RoomSummary
import data.io.matrix.room.event.ConversationRoomMember
import data.io.social.network.conversation.message.ConversationAnchorMessageIO
import data.io.social.network.conversation.message.ConversationMessageIO.VerificationRequestInfo
import data.io.social.network.conversation.message.MediaIO
import data.io.social.network.conversation.message.MessageReactionIO
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.serialization.json.Json
import net.folivo.trixnity.clientserverapi.model.sync.Sync.Response.Rooms.InvitedRoom
import net.folivo.trixnity.clientserverapi.model.sync.Sync.Response.Rooms.JoinedRoom.UnreadNotificationCounts
import net.folivo.trixnity.clientserverapi.model.sync.Sync.Response.Rooms.KnockedRoom.InviteState
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.PresenceEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.keys.EncryptionAlgorithm
import org.koin.mp.KoinPlatform
import ui.conversation.components.experimental.gravity.GravityData

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
        return value?.split(",")?.mapNotNull { it.toLongOrNull() }    }
    
    @TypeConverter
    fun fromRoomSummary(value: RoomSummary): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toRoomSummary(value: String): RoomSummary {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun fromVerificationRequestInfo(value: VerificationRequestInfo): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toVerificationRequestInfo(value: String): VerificationRequestInfo {
        return json.decodeFromString(value)
    }
    
    @TypeConverter
    fun fromPresenceEventContent(value: PresenceEventContent): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toPresenceEventContent(value: String): PresenceEventContent {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun fromConversationRoomMember(value: ConversationRoomMember): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toConversationRoomMember(value: String): ConversationRoomMember {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun fromGravityData(value: GravityData): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toGravityData(value: String): GravityData {
        return json.decodeFromString(value)
    }
    
    @TypeConverter
    fun fromEncryptionAlgorithm(value: EncryptionAlgorithm): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toMemberEventContent(value: String): MemberEventContent {
        return json.decodeFromString(value)
    }
    @TypeConverter
    fun fromMemberEventContent(value: MemberEventContent): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toEncryptionAlgorithm(value: String): EncryptionAlgorithm {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun fromUserId(value: UserId): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toUserId(value: String): UserId {
        return json.decodeFromString(value)
    }
    
    @TypeConverter
    fun fromInvitedInviteState(value: InviteState?): String? {
        return if(value == null) null else json.encodeToString(value)
    }
    
    @TypeConverter
    fun toInvitedInviteState(value: String?): InviteState? {
        return if(value == null) null else json.decodeFromString(value)
    }

    @TypeConverter
    fun fromInviteState(value: InvitedRoom.InviteState?): String? {
        return if(value == null) null else json.encodeToString(value)
    }

    @TypeConverter
    fun toInviteState(value: String?): InvitedRoom.InviteState? {
        return if(value == null) null else json.decodeFromString(value)
    }

    @TypeConverter
    fun fromUnreadNotificationCounts(value: UnreadNotificationCounts?): String? {
        return if(value == null) null else json.encodeToString(value)
    }

    @TypeConverter
    fun toUnreadNotificationCounts(value: String?): UnreadNotificationCounts? {
        return if(value == null) null else json.decodeFromString(value)
    }

    @TypeConverter
    fun fromMediaIO(value: MediaIO?): String? {
        return if(value == null) null else json.encodeToString(value)
    }

    @TypeConverter
    fun toMediaIO(value: String?): MediaIO? {
        return if(value == null) null else json.decodeFromString(value)
    }
    
    @TypeConverter
    fun fromMediaList(value: List<MediaIO>?): String? {
        return if(value.isNullOrEmpty()) null else json.encodeToString(value)
    }
    
    @TypeConverter
    fun toMediaList(value: String?): List<MediaIO>? {
        return if(value == null) null else json.decodeFromString(value)
    }
    
    @TypeConverter
    fun fromReactionList(value: List<MessageReactionIO>?): String? {
        return if(value.isNullOrEmpty()) null else json.encodeToString(value)
    }
    
    @TypeConverter
    fun toReactionList(value: String?): List<MessageReactionIO>? {
        return if(value == null) null else json.decodeFromString(value)
    }
    
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime): String {
        return value.format(LocalDateTime.Formats.ISO)
    }
    
    @TypeConverter
    fun toLocalDateTime(value: String): LocalDateTime {
        return LocalDateTime.parse(value, LocalDateTime.Formats.ISO)
    }
    
    @TypeConverter
    fun fromConversationAnchorMessageIO(value: ConversationAnchorMessageIO): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toConversationAnchorMessageIO(value: String): ConversationAnchorMessageIO? {
        return json.decodeFromString(value)
    }}