@file:OptIn(ExperimentalSettingsApi::class)

package ui.conversation.components.emoji

import augmy.composeapp.generated.resources.Res
import augmy.interactive.shared.ext.ifNull
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.FlowSettings
import data.io.app.SettingsKeys
import data.io.social.network.conversation.EmojiData
import data.io.social.network.conversation.EmojiSelection
import data.shared.SharedDataManager
import database.dao.EmojiSelectionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.dsl.module

internal val emojiModule = module {
    factory { EmojiDataManager() }
    single { EmojiDataManager() }
    factory { EmojiUseCase(get(), get(), get(), get(), get()) }
}

/** Bundle for handling, retrieving, and modifying emojis within the app */
@OptIn(ExperimentalResourceApi::class)
class EmojiUseCase(
    private val dataManager: EmojiDataManager,
    private val sharedDataManager: SharedDataManager,
    private val json: Json,
    private val settings: FlowSettings,
    private val selectionDao: EmojiSelectionDao
) {
    private val emojiHistory = MutableStateFlow(mutableListOf<EmojiSelection>())
    private val emojiSearch = MutableStateFlow("")

    /** user preferred emojis, manually selected without underlying algorithm */
    val preferredEmojis = MutableStateFlow(listOf<EmojiData>())

    /** Whether there is emoji filter */
    val areEmojisFiltered = MutableStateFlow(false)

    /** List of all available emojis */
    val emojis = dataManager.emojis.combine(emojiHistory) { emojis, history ->
        if(emojis.isEmpty()) return@combine null
        withContext(Dispatchers.Default) {
            mutableListOf(
                EMOJIS_HISTORY_GROUP to history.distinctBy { it.name }.map {
                    EmojiData(emoji = mutableListOf(it.content ?: ""), name = "'" + it.name)
                }
            ).apply {
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

    suspend fun initialize(conversationId: String?) {
        if(preferredEmojis.value.isEmpty()) {
            requestPreferredEmojis()
        }
        requestEmojiSelections(conversationId = conversationId)

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

    /** Takes a note about an emoji selection */
    suspend fun noteEmojiSelection(
        emoji: EmojiData,
        conversationId: String?
    ) {
        withContext(Dispatchers.IO) {
            // update conversation specific selection
            if(conversationId != null) {
                selectionDao.insertSelection(
                    (emojiHistory.value.find {
                        it.conversationId == conversationId && it.name == emoji.name
                    } ?: EmojiSelection(
                        name = emoji.name,
                        conversationId = conversationId,
                        content = emoji.emoji.first(),
                        count = 0
                    )).apply {
                        count += 1
                    }
                )
            }

            // update general emoji selection
            selectionDao.insertSelection(
                (dataManager.emojiGeneralHistory.value.find { it.name == emoji.name } ?: EmojiSelection(
                    name = emoji.name,
                    content = emoji.emoji.first(),
                    count = 0
                ).also {
                    dataManager.emojiGeneralHistory.value.add(it)
                }).apply {
                    count += 1
                }
            )
        }
    }

    /** Retrieves emoji selections from the database */
    private suspend fun requestEmojiSelections(conversationId: String?) {
        withContext(Dispatchers.IO) {
            if(dataManager.emojiGeneralHistory.value.isEmpty()) {
                dataManager.emojiGeneralHistory.value = selectionDao.getGeneralSelections().toMutableList()
            }
            emojiHistory.value = selectionDao.getSelections(
                conversationId = conversationId ?: ""
            ).toMutableList()

            // fill in missing emojis from general history,
            // general history never overrides conversation specific history in order!
            emojiHistory.value.addAll(dataManager.emojiGeneralHistory.value.take(
                EMOJIS_HISTORY_LENGTH - emojiHistory.value.size
            ))
        }
    }

    /** Updates preferred emojis */
    suspend fun updatePreferredEmojiSet(list: List<EmojiData>) {
        withContext(Dispatchers.IO) {
            preferredEmojis.value = list
            settings.putString(
                "${SettingsKeys.KEY_PREFERRED_EMOJIS}_${sharedDataManager.currentUser.value?.publicId}",
                json.encodeToString(list)
            )
        }
    }

    /** Makes a request for preferred emojis */
    private suspend fun requestPreferredEmojis() {
        withContext(Dispatchers.IO) {
            preferredEmojis.value = settings.getStringOrNull(
                "${SettingsKeys.KEY_PREFERRED_EMOJIS}_${sharedDataManager.currentUser.value?.publicId}"
            )?.let { jsonString ->
                json.decodeFromString<List<EmojiData>>(jsonString)
            }.ifNull {
                DefaultEmojis
            }
        }
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

        internal const val EMOJIS_HISTORY_LENGTH = 30
    }
}