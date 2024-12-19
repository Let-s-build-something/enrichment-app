package database

import androidx.room.Room
import androidx.room.RoomDatabase
import database.AppRoomDatabase.Companion.DATABASE_NAME
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** returns database builder specific to each platform */
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppRoomDatabase> {
    val dbFilePath = documentDirectory() + DATABASE_NAME
    return Room.databaseBuilder<AppRoomDatabase>(
        name = dbFilePath,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}