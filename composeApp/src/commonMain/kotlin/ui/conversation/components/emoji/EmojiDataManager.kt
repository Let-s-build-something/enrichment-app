package ui.conversation.components.emoji

import data.io.social.network.conversation.EmojiData
import kotlinx.coroutines.flow.MutableStateFlow

/** Stores data relevant to the conversation */
class EmojiDataManager {

    /** List of all available emojis */
    val emojis = MutableStateFlow(listOf<Pair<String, List<EmojiData>>>())
}