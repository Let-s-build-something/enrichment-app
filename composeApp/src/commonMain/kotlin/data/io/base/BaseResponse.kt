package data.io.base

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

sealed class BaseResponse<out T> {

    data object Loading: BaseResponse<Nothing>()

    data class Success<out T>(val data: T) : BaseResponse<T>()

    @Serializable
    data class Error(
        /** list of error objects */
        val errors: List<String> = listOf()
    ): BaseResponse<Nothing>() {
        var httpCode: Int = -1
    }

    /** returns this object as a success */
    val success: Success<T>?
        get() = this as? Success<T>

    /** returns this object as an error */
    val error : Error?
        get() = this as? Error

    companion object {
        suspend inline fun <reified T> HttpResponse.getResponse(): BaseResponse<T> {
            return when (status) {
                HttpStatusCode.OK,
                HttpStatusCode.Created,
                HttpStatusCode.Accepted,
                HttpStatusCode.NonAuthoritativeInformation -> Success(this.body<T>())
                else -> try {
                    this.body<Error>().apply {
                        httpCode = status.value
                    }
                }catch (e: NoTransformationFoundException) {
                    Error()
                }
            }
        }
    }
}