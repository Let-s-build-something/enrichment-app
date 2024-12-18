package database

import androidx.room.Room
import androidx.room.RoomDatabase
import database.AppRoomDatabase.Companion.DATABASE_NAME
import java.io.File

/** returns database builder specific to each platform */
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppRoomDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), DATABASE_NAME)
    return Room.databaseBuilder<AppRoomDatabase>(
        name = dbFile.absolutePath,
    )
}