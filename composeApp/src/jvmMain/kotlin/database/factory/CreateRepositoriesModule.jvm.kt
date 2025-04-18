package database.factory

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import base.utils.RootPath
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.folivo.trixnity.client.store.repository.room.TrixnityRoomDatabase
import net.folivo.trixnity.client.store.repository.room.createRoomRepositoriesModule
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.crypto.core.SecureRandom
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun platformCreateRepositoriesModuleModule(): Module = module {
    single<CreateRepositoriesModule> {
        val rootPath = get<RootPath>()
        val convertSecretByteArray = get<ConvertSecretByteArray>()

        object : CreateRepositoriesModule {
            override suspend fun create(userId: UserId): CreateRepositoriesModule.CreateResult {
                FileSystem.SYSTEM.createDirectories(rootPath.forAccountDatabase(userId), mustCreate = false)
                val databaseKey = SecureRandom.nextBytes(EncryptedSQLiteDriver.KEY_SIZE)
                return CreateRepositoriesModule.CreateResult(
                    module = createRoomRepositoriesModule(db(userId, databaseKey)),
                    databaseKey = convertSecretByteArray(databaseKey)
                )
            }

            override suspend fun load(
                userId: UserId,
                databaseKey: SecretByteArray?,
            ): Module {
                val existingKey = databaseKey?.let { convertSecretByteArray(it) }
                return createRoomRepositoriesModule(db(userId, existingKey))
            }

            private fun db(userId: UserId, databaseKey: ByteArray?): RoomDatabase.Builder<TrixnityRoomDatabase> =
                Room.databaseBuilder<TrixnityRoomDatabase>(
                    rootPath.forAccountDatabase(userId).resolve("database").toString()
                ).apply {
                    setDriver(
                        databaseKey?.let(::EncryptedSQLiteDriver)
                            ?: throw MatrixClientInitializationException.DatabaseAccessException("No Encryption Key given")
                    )
                }
        }
    }
}

private class EncryptedSQLiteDriver(key: ByteArray) : SQLiteDriver {


    companion object {
        const val KEY_SIZE = 32
        val mutex = Mutex()
    }

    init {
        if (key.size != KEY_SIZE) {
            throw MatrixClientInitializationException.DatabaseAccessException("Invalid key size: want ${KEY_SIZE}, got ${key.size}")
        }
    }

    @ExperimentalStdlibApi
    private val rawKey = key.toHexString()

    private val driver = BundledSQLiteDriver()

    @ExperimentalStdlibApi
    override fun open(fileName: String): SQLiteConnection = runBlocking {
        mutex.withLock {
            driver.open(fileName).apply {
                /*TODO enable with sqlcipher encryption  prepare("PRAGMA key = 'raw:$rawKey'").use {
                    if (!it.step() || it.getColumnNames().getOrNull(0) != "ok")
                        throw MatrixClientInitializationException.DatabaseAccessException("Database does not support Encryption")
                }*/
            }
        }
    }
}