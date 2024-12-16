package ui.conversation.components.gif

import androidx.paging.PagingData
import data.io.social.network.conversation.giphy.GiphyData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Stores data relevant to the conversation */
class GifDataManager {

    /** Paginated list of currently trending gifs */
    val trendingGifs = MutableStateFlow<Flow<PagingData<GiphyData>>?>(null)
}