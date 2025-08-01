package database.factory

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import base.utils.RootPath
import net.folivo.trixnity.client.store.repository.room.TrixnityRoomDatabase
import net.folivo.trixnity.client.store.repository.room.createRoomRepositoriesModule
import net.folivo.trixnity.core.model.UserId
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module
import utils.SharedLogger

internal actual fun platformCreateRepositoriesModuleModule(): Module = module {
    single<CreateRepositoriesModule> {
        val rootPath = get<RootPath>()

        object : CreateRepositoriesModule {
            override suspend fun create(userId: UserId): CreateRepositoriesModule.CreateResult {
                val dir = rootPath.forAccountDatabase(userId)
                SharedLogger.logger.debug { "CreateRepositoriesModule: create($userId), directory: $dir" }
                FileSystem.SYSTEM.createDirectories(dir, mustCreate = false)
                return CreateRepositoriesModule.CreateResult(
                    module = createRoomRepositoriesModule(db(userId)),
                    databaseKey = null,
                )
            }

            override suspend fun load(
                userId: UserId,
                databaseKey: SecretByteArray?,
            ): Module {
                return createRoomRepositoriesModule(db(userId))
            }

            private fun db(userId: UserId): RoomDatabase.Builder<TrixnityRoomDatabase> {
                SharedLogger.logger.debug { "Creating database for $userId (name: database)" }
                return Room.databaseBuilder<TrixnityRoomDatabase>(
                    rootPath.forAccountDatabase(userId).resolve("database").toString()
                ).apply {
                    setDriver(BundledSQLiteDriver())
                }
            }
        }
    }
}
