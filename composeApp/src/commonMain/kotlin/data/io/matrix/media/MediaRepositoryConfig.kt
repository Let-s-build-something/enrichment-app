package data.io.matrix.media

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaRepositoryConfig(
    /** The maximum size an upload can be in bytes. */
    @SerialName("m.upload.size")
    val maxUploadSize: Long
)