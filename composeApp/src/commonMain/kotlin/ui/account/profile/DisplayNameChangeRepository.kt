package ui.account.profile

import base.utils.toSha256
import data.io.base.BaseResponse
import data.io.matrix.media.MediaUploadResponse
import data.io.social.username.RequestUserPropertiesChange
import data.io.social.username.ResponseDisplayNameChange
import database.file.FileAccess
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ui.login.safeRequest

/** Class for making DB requests */
class DisplayNameChangeRepository(
    private val httpClient: HttpClient,
    private val fileAccess: FileAccess
) {

    /** Makes a request to change username */
    suspend fun changeDisplayName(value: CharSequence): BaseResponse<ResponseDisplayNameChange> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<ResponseDisplayNameChange> {
                patch(
                    urlString = "/api/v1/users",
                    block =  {
                        setBody(RequestUserPropertiesChange(displayName = value.toString()))
                    }
                )
            }
        }
    }

    /** Validates input value by user */
    suspend fun validateDisplayName(value: CharSequence): BaseResponse<Any> {
        return withContext(Dispatchers.IO) {
            httpClient.safeRequest<Any> {
                get(urlString = "/api/v1/users/validate/display-name?value=$value")
            }
        }
    }

    /**
     * Uploads media to the server
     * @return the server location URL
     */
    suspend fun uploadMedia(
        mediaByteArray: ByteArray?,
        mimetype: String,
        homeserver: String,
        fileName: String
    ): BaseResponse<MediaUploadResponse> = withContext(Dispatchers.IO) {
        try {
            if(mediaByteArray == null) BaseResponse.Error()
            else {
                httpClient.safeRequest<MediaUploadResponse> {
                    post(url = Url("https://$homeserver/_matrix/media/v3/upload")) {
                        header(HttpHeaders.ContentType, mimetype)
                        parameter("filename", fileName)
                        setBody(mediaByteArray)
                    }
                }.also {
                    it.success?.success?.data?.contentUri?.let { uri ->
                        fileAccess.saveFileToCache(
                            data = mediaByteArray,
                            fileName = uri.toSha256()
                        )
                    }
                }
            }
        }catch (e: Exception) {
            e.printStackTrace()
            BaseResponse.Error()
        }
    }
}