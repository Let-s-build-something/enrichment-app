package data.io.matrix.room.event.content.key

import data.io.matrix.room.event.content.Mentions
import data.io.matrix.room.event.content.RelatesTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @see <a href="https://spec.matrix.org/unstable/client-server-api/#mkeyverificationready">matrix spec</a>
 */
@Serializable
data class VerificationReadyEventContent(
    @SerialName("from_device")
    val fromDevice: String,
    @SerialName("methods")
    val methods: Set<VerificationMethod>,
    @SerialName("m.relates_to")
    override val relatesTo: RelatesTo.Reference?,
    @SerialName("transaction_id")
    override val transactionId: String?,
) : VerificationStep {
    override val mentions: Mentions? = null
    override val externalUrl: String? = null
}