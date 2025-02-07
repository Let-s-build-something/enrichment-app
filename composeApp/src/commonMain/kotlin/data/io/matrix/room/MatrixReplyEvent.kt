package data.io.matrix.room

import kotlinx.serialization.Serializable

@Serializable
data class MatrixReplyEvent(
    val eventId: String? = null
)