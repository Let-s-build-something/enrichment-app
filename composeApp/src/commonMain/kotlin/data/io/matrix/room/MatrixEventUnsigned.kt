package data.io.matrix.room

import kotlinx.serialization.Serializable

@Serializable
data class MatrixEventUnsigned(
    /** The time in milliseconds that has elapsed since the event was sent. */
    val age: Long? = null,

    /** The client-supplied transaction ID. */
    val transactionId: String? = null
)