package database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import data.io.social.network.conversation.EmojiSelection
import data.io.user.NetworkItemIO
import database.dao.EmojiSelectionDao
import database.dao.NetworkItemDao

@Database(
    entities = [
        NetworkItemIO::class,
        EmojiSelection::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(AppDatabaseConverter::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppRoomDatabase: RoomDatabase() {

    /** An interface for interacting with local database for collections */
    abstract fun networkItemDbDao(): NetworkItemDao
    abstract fun emojiSelectionDao(): EmojiSelectionDao


    companion object {
        /** File name of the main database */
        const val DATABASE_NAME = "app_database.db"

        /** Identification of table for [NetworkItemIO] */
        const val ROOM_NETWORK_ITEM_TABLE = "room_network_item_table"

        /** Identification of table for [EmojiSelection] */
        const val ROOM_EMOJI_SELECTION_TABLE = "room_emoji_selection"
    }
}

/** Master database of this application */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppRoomDatabase> {
    override fun initialize(): AppRoomDatabase
}

/** returns database builder specific to each platform */
expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppRoomDatabase>
