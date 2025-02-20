package data.io.matrix.room.event.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InitialStateEvent<C : StateEventContent>(
    @SerialName("content") override val content: C,
    @SerialName("state_key") val stateKey: String
) : Event<C>