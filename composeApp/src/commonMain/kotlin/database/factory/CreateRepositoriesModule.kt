package database.factory

import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module

interface CreateRepositoriesModule {
    suspend fun create(userId: UserId): CreateResult
    suspend fun load(userId: UserId, databaseKey: SecretByteArray?): Module

    data class CreateResult(
        val module: Module,
        val databaseKey: SecretByteArray?,
    )
}

internal expect fun platformCreateRepositoriesModuleModule(): Module