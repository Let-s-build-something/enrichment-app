package data.io.social.network.conversation

import kotlinx.serialization.Serializable

@Serializable
data class EmojiData(
    val emoji: MutableList<String>,
    val name: String
)