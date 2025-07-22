package ui.conversation.search

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import base.utils.MediaType
import data.shared.SharedModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private val _selectedMediaTypes = MutableStateFlow(listOf<MediaType>())

    val selectedMediaTypes = _selectedMediaTypes.asStateFlow()

    val messages = repository.searchForMessages(
        query = { _query.value },
        selectedMediaTypes = { _selectedMediaTypes.value },
        config = PagingConfig(
            pageSize = PAGE_ITEM_COUNT,
            enablePlaceholders = true
        ),
        homeserver = { homeserverAddress },
        conversationId = conversationId
    ).flow.cachedIn(viewModelScope)


    fun querySearch(query: CharSequence) {
        _query.value = query.toString()
        repository.invalidateLocalSource()
    }

    fun selectMediaType(type: MediaType) {
        _selectedMediaTypes.update {
            if (it.contains(type)) it.minus(type)
            else it.plus(type)
        }
        repository.invalidateLocalSource()
    }
}