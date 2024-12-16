package ui.conversation.components.giphy

import androidx.compose.ui.text.intl.Locale
import androidx.paging.Pager
import androidx.paging.PagingConfig
import augmy.interactive.com.BuildKonfig
import data.io.base.BaseResponse
import data.io.social.network.conversation.giphy.GiphyData
import data.io.social.network.conversation.giphy.GiphyPageResponse
import data.shared.ApiConstants
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.login.safeRequest

/** Class for calling APIs and remote work in general */
class GifRepository(private val httpClient: HttpClient) {

    /** returns a list of gifs based on a [section] */
    private suspend fun getGifs(
        offset: Int,
        size: Int,
        section: String,
        query: String? = null
    ): BaseResponse<GiphyPageResponse> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<GiphyPageResponse> {
                get(
                    urlString = buildString {
                        append(ApiConstants.GIPHY_API_URL + "/v1/gifs/$section")
                        append("?api_key=${BuildKonfig.GiphyApiKey}")
                        append("&limit=$size")
                        append("&offset=$offset")
                        append("&rating=pg-2")
                        if(query != null) {
                            append("&q=$query")
                            append("&lang=${Locale.current.language}")
                        }
                    }
                )
            }
        }
    }

    /** Returns a flow of gifs */
    fun getGifListFlow(
        config: PagingConfig,
        section: String,
        query: String? = null
    ): Pager<Int, GiphyData> {
        return Pager(config) {
            GifSource(
                getGifs = { offset, size ->
                    getGifs(
                        offset = offset,
                        size = size,
                        section = section,
                        query = query
                    )
                },
                size = config.pageSize
            )
        }
    }

    companion object {
        const val TRENDING_SECTION = "trending"
        const val SEARCH_SECTION = "search"
    }
}