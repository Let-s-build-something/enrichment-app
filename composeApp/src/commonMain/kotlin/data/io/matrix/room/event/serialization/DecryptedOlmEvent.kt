package data.io.matrix.room.event.serialization

import data.io.matrix.room.event.content.Event
import data.io.matrix.room.event.content.EventContent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.keys.Keys

@Serializable
data class DecryptedOlmEvent<C : EventContent>(
    @SerialName("content") override val content: C,
    @SerialName("sender") val sender: UserId,
    @SerialName("keys") val senderKeys: Keys,
    @SerialName("recipient") val recipient: UserId,
    @SerialName("recipient_keys") val recipientKeys: Keys
) : Event<C>