package data.io.social.network.conversation.matrix

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatrixEventUnsigned(
    /** The time in milliseconds that has elapsed since the event was sent. */
    val age: Long? = null,

    /** The client-supplied transaction ID. */
    @SerialName("transaction_id")
    val transactionId: String? = null
)