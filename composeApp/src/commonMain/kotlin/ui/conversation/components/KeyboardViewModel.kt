package ui.conversation.components

import androidx.compose.animation.core.Animatable
import androidx.lifecycle.viewModelScope
import data.io.app.SettingsKeys
import data.io.social.network.conversation.EmojiData
import data.shared.SharedViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.dsl.module
import ui.conversation.components.emoji.EmojiUseCase
import ui.conversation.components.emoji.emojiModule
import ui.conversation.components.gif.GifRepository.Companion.SEARCH_SECTION
import ui.conversation.components.gif.GifRepository.Companion.TRENDING_SECTION
import ui.conversation.components.gif.GifUseCase
import ui.conversation.components.gif.gifModule

internal val keyboardModule = module {
    includes(emojiModule)
    includes(gifModule)
}

/** Controller and provider of data specific to the conversation keyboard */
open class KeyboardViewModel(
    private val emojiUseCase: EmojiUseCase,
    private val gifUseCase: GifUseCase
): SharedViewModel() {

    init {
        viewModelScope.launch {
            emojiUseCase.initialize()
        }
    }

    val additionalBottomPadding = Animatable(0f)

    /** Last height of soft keyboard */
    var keyboardHeight: Int = settings.getIntOrNull(SettingsKeys.KEY_KEYBOARD_HEIGHT) ?: 0
        set(value) {
            field = value
            settings.putInt(SettingsKeys.KEY_KEYBOARD_HEIGHT, value)
        }

    /** Whether hint about emoji preference should be displayed */
    var showEmojiPreferenceHint: Boolean = settings.getBooleanOrNull(SettingsKeys.KEY_SHOW_EMOJI_PREFERENCE_HINT) ?: true
        set(value) {
            field = value
            settings.putBoolean(SettingsKeys.KEY_SHOW_EMOJI_PREFERENCE_HINT, value)
        }

    /** Preferred emojis of an individual conversation */
    val preferredEmojis = emojiUseCase.preferredEmojis.asStateFlow()

    /** List of all available emojis */
    val emojis = emojiUseCase.emojis

    /** Whether there is emoji filter */
    val areEmojisFiltered = emojiUseCase.areEmojisFiltered.asStateFlow()

    /** Paginated list of currently trending gifs */
    val trendingGifs = gifUseCase.trendingGifs

    /** Paginated list of searched gifs */
    val queriedGifs = gifUseCase.queriedGifs



    /** Makes a request to retrieve trending GIFs */
    fun requestTrendingGifs() {
        viewModelScope.launch {
            gifUseCase.requestGifs(
                coroutineScope = viewModelScope,
                section = TRENDING_SECTION
            )
        }
    }

    /** Makes a request to retrieve trending GIFs */
    fun requestGifSearch(query: String) {
        viewModelScope.launch {
            gifUseCase.requestGifs(
                coroutineScope = viewModelScope,
                section = SEARCH_SECTION,
                query = query
            )
        }
    }

    /** Filters emojis */
    fun filterEmojis(query: String) {
        viewModelScope.launch {
            emojiUseCase.filterEmojis(query)
        }
    }

    /** Updates preferred emojis */
    fun updatePreferredEmojiSet(list: List<EmojiData>) {
        viewModelScope.launch {
            emojiUseCase.updatePreferredEmojiSet(list)
        }
    }
}