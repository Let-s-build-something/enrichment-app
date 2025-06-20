package ui.conversation.components.experimental.gravity

import kotlinx.serialization.Serializable

@Serializable
data class GravityData(
    /** Asc sorted recorded gravity values */
    val values: List<GravityValue>,

    /** How many milliseconds each value represents */
    val tickMs: Long
)