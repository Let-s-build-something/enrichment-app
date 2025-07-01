package ui.conversation.search

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import data.shared.SharedModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ui.conversation.settings.ConversationSettingsModel.Companion.PAGE_ITEM_COUNT

internal val conversationSearchModule = module {
    factory { ConversationSearchRepository(get()) }

    factory { (conversationId: String?) ->
        ConversationSearchModel(conversationId ?: "", get())
    }
    viewModel { (conversationId: String?) ->
        ConversationSearchModel(conversationId ?: "", get())
    }
}

class ConversationSearchModel(
    val conversationId: String?,
    private val repository: ConversationSearchRepository
): SharedModel() {
    private val _query = MutableStateFlow("")

    val messages = repository.searchForMessages(
        query = { _query.value },
        config = PagingConfig(
            pageSize = PAGE_ITEM_COUNT,
            enablePlaceholders = true
        ),
        homeserver = { homeserver },
        conversationId = conversationId
    ).flow.cachedIn(viewModelScope)


    fun querySearch(query: CharSequence) {
        _query.value = query.toString()
        repository.invalidateLocalSource()
    }
}