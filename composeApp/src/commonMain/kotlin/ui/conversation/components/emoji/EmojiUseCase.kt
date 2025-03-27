@file:OptIn(ExperimentalUuidApi::class)

package ui.conversation.components.emoji

import augmy.composeapp.generated.resources.Res
import data.io.app.SettingsKeys
import data.io.social.network.conversation.EmojiData
import data.io.social.network.conversation.EmojiSelection
import data.shared.SharedDataManager
import database.dao.EmojiSelectionDao
import koin.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.dsl.module
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
    private val settings: AppSettings,
    private val selectionDao: EmojiSelectionDao
) {
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

    private val conversationEmojiHistory = MutableStateFlow<EmojiHistory?>(null)
    private val emojiSearch = MutableStateFlow("")

    /** user preferred emojis, manually selected without underlying algorithm */
    val preferredEmojis = MutableStateFlow(listOf<EmojiData>())

    /** Whether there is emoji filter */
    val areEmojisFiltered = MutableStateFlow(false)

    private val emojiHistory = conversationEmojiHistory.combine(dataManager.emojiGeneralHistory) { history, generalHistory ->
        withContext(Dispatchers.Default) {
            EMOJIS_HISTORY_GROUP to mutableListOf(
                *history?.selections.orEmpty().sortedByDescending { it.count }.toTypedArray()
            )
                .apply {
                    var index = 0
                    while(size < EMOJIS_HISTORY_LENGTH && ++index < generalHistory.size) {
                        if(this.none { it.content == generalHistory[index].content }) {
                            add(generalHistory[index])
                        }
                    }
                }
                .map {
                    EmojiData(
                        emoji = mutableListOf(it.content ?: ""),
                        name = "${EMOJIS_HISTORY_GROUP}${it.name.removePrefix(EMOJIS_HISTORY_GROUP)}"
                    )
                }
        }
    }

    /** List of all available emojis */
    val emojis = dataManager.emojis.combine(emojiHistory) { emojis, history ->
        if(emojis.isEmpty()) {
            return@combine null
        }else listOf(history).plus(emojis)
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
            val emojiName = emoji.name.removePrefix(EMOJIS_HISTORY_GROUP)

            // update general emoji selection
            dataManager.emojiGeneralHistory.update { prev ->
                prev.apply {
                    selectionDao.insertSelection(
                        (find { it.name == emojiName } ?: EmojiSelection(
                            name = emojiName,
                            content = emoji.emoji.first(),
                            count = 0
                        )).also {
                            it.count += 1
                            if(it.count == 1) this@apply.add(it)
                            selectionDao.insertSelection(it)
                        }
                    )
                }
            }

            // update conversation specific selection
            if(conversationId != null) {
                conversationEmojiHistory.value?.apply {
                    selectionDao.insertSelection(
                        (selections.find { it.name == emojiName } ?: EmojiSelection(
                            name = emojiName,
                            conversationId = conversationId,
                            content = emoji.emoji.first(),
                            count = 0
                        )).also {
                            it.count += 1
                            if(it.count == 1) this@apply.selections.add(it)
                            selectionDao.insertSelection(it)
                        }
                    )
                }
                conversationEmojiHistory.value = EmojiHistory(
                    selections = conversationEmojiHistory.value?.selections.orEmpty().toMutableList()
                )
            }
        }
    }

    /** Retrieves emoji selections from the database */
    private suspend fun requestEmojiSelections(conversationId: String?) {
        withContext(Dispatchers.IO) {
            if(dataManager.emojiGeneralHistory.value.isEmpty()) {
                dataManager.emojiGeneralHistory.value = selectionDao.getGeneralSelections().toMutableList()
            }
            if(conversationEmojiHistory.value == null) {
                conversationEmojiHistory.value = EmojiHistory(
                    selections = selectionDao.getSelections(
                        conversationId = conversationId ?: ""
                    ).distinctBy { it.name }.sortedByDescending { it.count }.toMutableList()
                )
            }
        }
    }

    /** Updates preferred emojis */
    suspend fun updatePreferredEmojiSet(list: List<EmojiData>) {
        withContext(Dispatchers.IO) {
            preferredEmojis.value = list
            settings.putString(
                "${SettingsKeys.KEY_PREFERRED_EMOJIS}_${sharedDataManager.currentUser.value?.matrixUserId}",
                json.encodeToString(list)
            )
        }
    }

    /** Makes a request for preferred emojis */
    private suspend fun requestPreferredEmojis() {
        withContext(Dispatchers.IO) {
            preferredEmojis.value = settings.getStringOrNull(
                "${SettingsKeys.KEY_PREFERRED_EMOJIS}_${sharedDataManager.currentUser.value?.matrixUserId}"
            )?.let { jsonString ->
                json.decodeFromString<List<EmojiData>>(jsonString)
            } ?: DefaultEmojis
        }
    }

    private data class EmojiHistory(
        val selections: MutableList<EmojiSelection>,
        private val uid: String = Uuid.random().toString()
    ) {
        override fun toString(): String {
            return "{" +
                    "selections=$selections, " +
                    "uid=$uid" +
                    "}"
        }
    }
}