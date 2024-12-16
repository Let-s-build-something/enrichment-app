package ui.conversation.components.giphy

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import data.io.social.network.conversation.giphy.GiphyData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.koin.dsl.module

internal val gifModule = module {
    factory { GifDataManager() }
    factory { GifRepository(get()) }
    single { GifDataManager() }
    factory { GifUseCase(get(), get()) }
}

/** Bundled functionality of Gifs */
class GifUseCase(
    private val repository: GifRepository,
    private val dataManager: GifDataManager
) {
    private val _queriedGifs = MutableStateFlow<Flow<PagingData<GiphyData>>?>(null)

    /** Paginated list of currently trending gifs */
    val trendingGifs = dataManager.trendingGifs.asStateFlow()

    /** Paginated list of searched gifs */
    val queriedGifs = _queriedGifs.asStateFlow()

    /** Makes a request to retrieve GIFs */
    suspend fun requestGifs(
        coroutineScope: CoroutineScope,
        section: String,
        query: String? = null
    ) {
        withContext(Dispatchers.IO) {
            repository.getGifListFlow(
                config = PagingConfig(
                    pageSize = 20,
                    enablePlaceholders = true,
                    initialLoadSize = 20
                ),
                section = section,
                query = query
            ).flow.cachedIn(coroutineScope).let {
                if(query == null) {
                    dataManager.trendingGifs.value = it
                }else {
                    _queriedGifs.value = it
                }
            }
        }
    }
}
