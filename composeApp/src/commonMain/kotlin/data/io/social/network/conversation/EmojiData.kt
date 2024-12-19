package data.io.social.network.conversation

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class EmojiData(
    val emoji: MutableList<String>,
    val name: String
) {
    @OptIn(ExperimentalUuidApi::class)
    private val uid = Uuid.random().toString()

    constructor(content: String): this(
        emoji = mutableListOf(content),
        name = ""
    )

    override fun toString(): String {
        return "{" +
                "emoji=$emoji, " +
                "name=$name, " +
                "uid=$uid" +
                "}"
    }
}