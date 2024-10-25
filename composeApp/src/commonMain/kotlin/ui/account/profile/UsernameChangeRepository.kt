package ui.account.profile

import augmy.interactive.com.BuildKonfig
import data.io.base.BaseResponse
import data.io.base.BaseResponse.Companion.getResponse
import data.io.social.username.RequestUsernameChange
import data.io.social.username.ResponseUsernameChange
import dev.gitlive.firebase.storage.FirebaseStorageException
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.content.ByteArrayContent
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Class for making DB requests */
class UsernameChangeRepository(private val httpClient: HttpClient) {

    /** Makes a request to change username */
    suspend fun changeUsername(username: String): BaseResponse<ResponseUsernameChange> {
        return withContext(Dispatchers.IO) {
            httpClient.post(
                urlString = "v1/social/username",
                block =  {
                    setBody(RequestUsernameChange(username))
                }
            ).getResponse<ResponseUsernameChange>()
        }
    }

    suspend fun changeProfilePictureHttp(
        fileName: String,
        previousFileName: String,
        idToken: String,
        byteArray: ByteArray
    ): String? {
        val newUrl = postDataToBucket(
            fileName = fileName,
            idToken = idToken,
            byteArray = byteArray
        )
        //TODO somehow retrieve url
        if(newUrl) {
            deleteDataFromBucket(
                fileName = previousFileName,
                idToken = idToken,
                byteArray = byteArray
            )
        }

        return null
    }

    /** Makes a request to post new data to a storage bucket */
    private suspend fun postDataToBucket(
        fileName: String,
        idToken: String,
        byteArray: ByteArray
    ): Boolean {
        return withContext(Dispatchers.IO) {
            httpClient.post(
                urlString = "https://storage.googleapis.com/upload/storage/v1/b/${BuildKonfig.StorageBucketName}/o?uploadType=media&name=$fileName",
                block =  {
                    header(HttpHeaders.Authorization, "Bearer $idToken")
                    setBody(ByteArrayContent(byteArray))
                }
            ).also {
                println(it.bodyAsText())
            }.status.isSuccess()
        }
    }

    /** Makes a request to delete existing data from a storage bucket */
    private suspend fun deleteDataFromBucket(
        fileName: String,
        idToken: String,
        byteArray: ByteArray
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                httpClient.delete(
                    urlString = "https://storage.googleapis.com/upload/storage/v1/b/${BuildKonfig.StorageBucketName}/o?uploadType=media&name=$fileName",
                    block =  {
                        header(HttpHeaders.Authorization, "Bearer $idToken")
                        setBody(ByteArrayContent(byteArray))
                    }
                ).also {
                    println(it.bodyAsText())
                }.status.isSuccess()
            }catch (e: FirebaseStorageException) {
                false
            }
        }
    }
}