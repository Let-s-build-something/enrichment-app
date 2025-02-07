package database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import data.io.base.paging.MatrixPagingMetaIO
import data.io.base.paging.PagingMetaIO
import data.io.matrix.room.ConversationRoomIO
import data.io.social.network.conversation.EmojiSelection
import data.io.social.network.conversation.message.ConversationMessageIO
import data.io.user.NetworkItemIO
import database.dao.ConversationMessageDao
import database.dao.ConversationRoomDao
import database.dao.EmojiSelectionDao
import database.dao.MatrixPagingMetaDao
import database.dao.NetworkItemDao
import database.dao.PagingMetaDao

@Database(
    entities = [
        NetworkItemIO::class,
        EmojiSelection::class,
        PagingMetaIO::class,
        MatrixPagingMetaIO::class,
        ConversationMessageIO::class,
        ConversationRoomIO::class
    ],
    version = 25,
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
    abstract fun matrixPagingMetaDao(): MatrixPagingMetaDao


    companion object {
        /** File name of the main database */
        const val DATABASE_NAME = "app_database.db"

        /** Identification of table for [NetworkItemIO] */
        const val ROOM_NETWORK_ITEM_TABLE = "room_network_item_table"

        /** Identification of table for [ConversationRoomIO] */
        const val ROOM_CONVERSATION_ROOM_TABLE = "room_conversation_room_table"

        /** Identification of table for [ConversationMessageIO] */
        const val ROOM_CONVERSATION_MESSAGE_TABLE = "room_conversation_message_table"

        /** Identification of table for [EmojiSelection] */
        const val ROOM_EMOJI_SELECTION_TABLE = "room_emoji_selection"

        /** Identification of table for [PagingMetaIO] */
        const val ROOM_PAGING_META_TABLE = "room_paging_meta_table"

        /** Identification of table for [MatrixPagingMetaIO] */
        const val ROOM_MATRIX_PAGING_META_TABLE = "room_matrix_paging_meta_table"
    }
}

/** Master database of this application */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppRoomDatabase> {
    override fun initialize(): AppRoomDatabase
}

/** returns database builder specific to each platform */
expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppRoomDatabase>
