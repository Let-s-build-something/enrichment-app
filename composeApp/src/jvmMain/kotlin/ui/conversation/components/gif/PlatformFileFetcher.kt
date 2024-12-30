package ui.conversation.components.gif

import base.utils.PlatformFileShell
import io.kamel.core.DataSource
import io.kamel.core.Resource
import io.kamel.core.config.ResourceConfig
import io.kamel.core.fetcher.Fetcher
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.reflect.KClass

val PlatformFileFetcher = object : Fetcher<PlatformFileShell> {

    override val inputDataKClass: KClass<PlatformFileShell> = PlatformFileShell::class

    override val source: DataSource = DataSource.Disk

    override val PlatformFileShell.isSupported: Boolean
        get() = true

    override fun fetch(
        data: PlatformFileShell,
        resourceConfig: ResourceConfig
    ): Flow<Resource<ByteReadChannel>> = flow {
        val byteReadChannel = ByteReadChannel(data.content.readBytes())
        emit(Resource.Success(byteReadChannel, source))
    }
}
