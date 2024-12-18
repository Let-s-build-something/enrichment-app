package database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import database.AppRoomDatabase.Companion.DATABASE_NAME
import org.koin.mp.KoinPlatform.getKoin

/** returns database builder specific to each platform */
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppRoomDatabase> {
    val applicationContext: Context = getKoin().get<Context>().applicationContext

    val dbFile = applicationContext.getDatabasePath(DATABASE_NAME)
    return Room.databaseBuilder<AppRoomDatabase>(
        context = applicationContext,
        name = dbFile.absolutePath
    )
}