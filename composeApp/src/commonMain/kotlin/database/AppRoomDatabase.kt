package database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import data.io.base.paging.MatrixPagingMetaIO
import data.io.base.paging.PagingMetaIO
import data.io.matrix.crypto.OutdatedKey
import data.io.matrix.crypto.StoredInboundMegolmMessageIndexEntity
import data.io.matrix.crypto.StoredInboundMegolmSessionEntity
import data.io.matrix.crypto.StoredOlmSessionEntity
import data.io.matrix.crypto.StoredOutboundMegolmSessionEntity
import data.io.matrix.room.ConversationRoomIO
import data.io.matrix.room.event.ConversationRoomMember
import data.io.social.network.conversation.EmojiSelection
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.user.NetworkItemIO
import data.io.user.PresenceData
import data.shared.crypto.model.KeyChainLink
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.EmojiSelectionDao
import database.dao.NetworkItemDao
import database.dao.PagingMetaDao
import database.dao.matrix.InboundMegolmSessionDao
import database.dao.matrix.KeyChainLinkDao
import database.dao.matrix.MatrixPagingMetaDao
import database.dao.matrix.MegolmMessageIndexDao
import database.dao.matrix.OlmSessionDao
import database.dao.matrix.OutboundMegolmSessionDao
import database.dao.matrix.OutdatedKeyDao
import database.dao.matrix.PresenceEventDao
import database.dao.matrix.RoomMemberDao
import net.folivo.trixnity.core.model.events.m.PresenceEventContent

@Database(
    entities = [
        NetworkItemIO::class,
        EmojiSelection::class,
        PagingMetaIO::class,
        MatrixPagingMetaIO::class,
        ConversationMessageIO::class,
        StoredOlmSessionEntity::class,
        StoredOutboundMegolmSessionEntity::class,
        StoredInboundMegolmSessionEntity::class,
        StoredInboundMegolmMessageIndexEntity::class,
        PresenceData::class,
        ConversationRoomMember::class,
        OutdatedKey::class,
        KeyChainLink::class,
        ConversationRoomIO::class
    ],
    version = 49,
    exportSchema = true
)
@TypeConverters(AppDatabaseConverter::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppRoomDatabase: RoomDatabase() {

    /** An interface for interacting with local database for collections */
    abstract fun networkItemDao(): NetworkItemDao
    abstract fun conversationMessageDao(): ConversationMessageDao
    abstract fun emojiSelectionDao(): EmojiSelectionDao
    abstract fun pagingMetaDao(): PagingMetaDao
    abstract fun conversationRoomDao(): ConversationRoomDao
    abstract fun presenceEventDao(): PresenceEventDao
    abstract fun matrixPagingMetaDao(): MatrixPagingMetaDao

    // crypto
    abstract fun olmSessionDao(): OlmSessionDao
    abstract fun outboundMegolmSessionDao(): OutboundMegolmSessionDao
    abstract fun inboundMegolmSessionDao(): InboundMegolmSessionDao
    abstract fun megolmMessageIndexDao(): MegolmMessageIndexDao
    abstract fun outdatedKeyDao(): OutdatedKeyDao
    abstract fun roomMemberDao(): RoomMemberDao
    abstract fun keyChainLinkDao(): KeyChainLinkDao


    companion object {
        /** File name of the main database */
        const val DATABASE_NAME = "app_database.db"

        /** Identification of table for [NetworkItemIO] */
        const val TABLE_NETWORK_ITEM = "room_network_item_table"

        /** Identification of table for [ConversationRoomIO] */
        const val TABLE_CONVERSATION_ROOM = "room_conversation_room_table"

        /** Identification of table for [ConversationMessageIO] */
        const val TABLE_CONVERSATION_MESSAGE = "room_conversation_message_table"

        /** Identification of table for [EmojiSelection] */
        const val TABLE_EMOJI_SELECTION = "room_emoji_selection"

        /** Identification of table for [PagingMetaIO] */
        const val TABLE_PAGING_META = "room_paging_meta_table"

        /** Identification of table for [MatrixPagingMetaIO] */
        const val TABLE_MATRIX_PAGING_META = "room_matrix_paging_meta_table"

        /** Identification of table for [PresenceEventContent] */
        const val TABLE_PRESENCE_EVENT = "presence_event_table"

        /** Identification of table for [StoredOlmSessionEntity] */
        const val TABLE_OLM_SESSION = "olm_session_table"

        /** Identification of table for [StoredOutboundMegolmSessionEntity] */
        const val TABLE_OUTBOUND_MEGOLM_SESSION = "outbound_megolm_session_table"

        /** Identification of table for [StoredInboundMegolmSessionEntity] */
        const val TABLE_INBOUND_MEGOLM_SESSION = "inbound_megolm_session_table"

        /** Identification of table for [StoredInboundMegolmSessionEntity] */
        const val TABLE_MEGOLM_MESSAGE_INDEX = "megolm_message_index_table"

        /** Identification of table for [OutdatedKey] */
        const val TABLE_OUTDATED_KEYS = "outdated_keys_table"

        /** Identification of table for [ConversationRoomMember] */
        const val TABLE_ROOM_MEMBER = "room_member_table"

        /** Identification of table for [KeyChainLink] */
        const val TABLE_KEY_CHAIN_LINK = "key_chain_link"
    }
}

/** Master database of this application */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppRoomDatabase> {
    override fun initialize(): AppRoomDatabase
}

/** returns database builder specific to each platform */
expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppRoomDatabase>
