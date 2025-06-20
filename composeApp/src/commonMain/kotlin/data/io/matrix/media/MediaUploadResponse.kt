package data.io.matrix.media

import kotlinx.serialization.Serializable

@Serializable
data class MediaUploadResponse(
    val contentUri: String? = null
)