package ui.search.user

import data.io.matrix.user.SearchUserRequest
import data.io.matrix.user.SearchUserResponse
import data.io.user.NetworkItemIO
import database.dao.NetworkItemDao
import database.dao.RoomMemberDao
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.login.safeRequest

class SearchUserRepository(
    private val httpClient: HttpClient,
    private val networkItemDao: NetworkItemDao,
    private val memberDao: RoomMemberDao
) {

    suspend fun queryForUsers(
        limit: Int,
        homeserver: String,
        prompt: String
    ) = withContext(Dispatchers.IO) {
        val localSearch = withContext(Dispatchers.Default) {
            networkItemDao.searchByPrompt(prompt = prompt)
                .plus(memberDao.searchByPrompt(prompt = prompt).map { it.toNetworkItem() })
                .distinctBy { it.userId }
        }

        if(localSearch.size < limit) {
            httpClient.safeRequest<SearchUserResponse> {
                post(url = Url("https://$homeserver/_matrix/client/v3/user_directory/search")) {
                    setBody(
                        SearchUserRequest(
                            limit = limit - localSearch.size,
                            searchTerm = prompt
                        )
                    )
                }
            }.success?.data?.results?.plus(localSearch)
        }else localSearch
    }

    suspend fun saveUser(user: NetworkItemIO) = withContext(Dispatchers.IO) {
        networkItemDao.insert(user)
    }
}
