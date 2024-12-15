package ui.conversation.components.emoji

import augmy.composeapp.generated.resources.Res
import data.io.social.network.conversation.EmojiData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import ui.conversation.EmojiDataManager

/** Bundle for handling, retrieving, and modifying emojis within the app */
@OptIn(ExperimentalResourceApi::class)
class EmojiUseCase(
    private val dataManager: EmojiDataManager,
    private val json: Json
) {
    val preferredEmojis = MutableStateFlow(listOf<EmojiData>())
    val emojiHistory = MutableStateFlow(mutableListOf<EmojiData>())
    val emojiSearch = MutableStateFlow("")

    /** Whether there is emoji filter */
    val areEmojisFiltered = MutableStateFlow(false)

    /** List of all available emojis */
    val emojis = dataManager.emojis.combine(emojiHistory) { emojis, history ->
        if(emojis.isEmpty()) return@combine null
        withContext(Dispatchers.Default) {
            mutableListOf(EMOJIS_HISTORY_GROUP to history.toList()).apply {
                addAll(emojis)
            }
        }
    }.combine(emojiSearch) { emojis, query ->
        if (query.isBlank()) {
            areEmojisFiltered.value = false
            emojis
        } else {
            areEmojisFiltered.value = true
            emojis?.map { category ->
                category.first to category.second.filter { emoji ->
                    emoji.name.contains(query, ignoreCase = true)
                }
            }
        }
    }

    suspend fun initialize() {
        if(preferredEmojis.value.isEmpty()) {
            requestPreferredEmojis()
        }

        if(dataManager.emojis.value.isEmpty()) {
            val jsonString = Res.readBytes("files/emoji_data_set.json").decodeToString()
            withContext(Dispatchers.Default) {
                json.decodeFromString<Map<String, Map<String, List<EmojiData>>>>(jsonString).let { raw ->
                    dataManager.emojis.value = raw.map { category ->
                        category.key to category.value.flatMap { it.value }
                    }
                }
            }
        }
    }

    /** Filters emojis */
    fun filterEmojis(query: String) {
        emojiSearch.value = query
    }

    /** Updates preferred emojis */
    suspend fun updatePreferredEmojiSet(list: List<EmojiData>) {
        preferredEmojis.value = list
        //TODO update preferred emojis local DB
    }

    /** Makes a request for preferred emojis */
    suspend fun requestPreferredEmojis() {
        preferredEmojis.value = DefaultEmojis
    }

    companion object {

        /** List of default emojis representing different categories */
        private val DefaultEmojis
            get() = listOf(
                EmojiData(mutableListOf("❤\uFE0F"), name = "Red heart"),
                EmojiData(mutableListOf("\uD83D\uDC4D"), name = "Thumbs up"),
                EmojiData(mutableListOf("\uD83D\uDC4E"), name = "Thumbs down"),
                EmojiData(mutableListOf("\uD83D\uDE06"), name = "Grinning Squinting Face"),
                EmojiData(mutableListOf("\uD83D\uDE2F"), name = "Hushed Face"),
                EmojiData(mutableListOf("\uD83D\uDE25"), name = "Sad but Relieved Face"),
            )

        /** Key for the group of emojis representing past history of this user */
        internal const val EMOJIS_HISTORY_GROUP = "history"
    }
}