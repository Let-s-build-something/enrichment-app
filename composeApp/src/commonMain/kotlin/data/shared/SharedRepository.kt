package data.shared

import data.io.user.UserIO
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

open class SharedRepository(private val httpClient: HttpClient) {

    /** Makes a request to create a user */
    suspend fun authenticateUser(): UserIO? {
        return withContext(Dispatchers.IO) {
            if(Firebase.auth.currentUser != null) {
                UserIO()
            }else null
        }
    }
}

/** sets URL parameters for paging */
fun HttpRequestBuilder.setPaging(
    page: Int,
    size: Int = 20
) = this.apply {
    parameters {
        append("page", page.toString())
        append("size", size.toString())
    }
}