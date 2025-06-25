package database

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module

internal val databaseModule = module {
    single<AppRoomDatabase> {
        getDatabaseBuilder()
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addTypeConverter(AppDatabaseConverter())
            .build()
    }

    // DAO providers
    factory { get<AppRoomDatabase>().networkItemDao() }
    factory { get<AppRoomDatabase>().emojiSelectionDao() }
    factory { get<AppRoomDatabase>().conversationMessageDao() }
    factory { get<AppRoomDatabase>().pagingMetaDao() }
    factory { get<AppRoomDatabase>().conversationRoomDao() }
    factory { get<AppRoomDatabase>().presenceEventDao() }
    factory { get<AppRoomDatabase>().matrixPagingMetaDao() }
    factory { get<AppRoomDatabase>().roomMemberDao() }
    factory { get<AppRoomDatabase>().messageReactionDao() }
    factory { get<AppRoomDatabase>().gravityDao() }
}
