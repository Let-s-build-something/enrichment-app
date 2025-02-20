package data.io.matrix.room.event.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.EventId

/**
 * @see <a href="https://spec.matrix.org/v1.10/client-server-api/#mroompinned_events">matrix spec</a>
 */
@Serializable
data class PinnedEventsEventContent(
    @SerialName("pinned")
    val pinned: List<EventId> = listOf(),
    @SerialName("external_url")
    override val externalUrl: String? = null,
) : StateEventContent