package ui.home.utils

import data.io.social.network.conversation.InvitationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.koin.dsl.module

internal val networkItemModule = module {
    factory { NetworkItemDataManager() }
    factory { NetworkItemRepository(get(), get(), get(), get()) }
    single { NetworkItemDataManager() }
    factory { NetworkItemUseCase(get(), get()) }
}

/** Bundled functionality of Gifs */
class NetworkItemUseCase(
    private val repository: NetworkItemRepository,
    private val dataManager: NetworkItemDataManager
) {
    private val _isLoading = MutableStateFlow(false)
    private val _invitationResponse = MutableStateFlow<InvitationResponse?>(null)

    val openConversations = dataManager.openConversations.asStateFlow()
    val networkItems = dataManager.networkItems.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val invitationResponse = _invitationResponse.asStateFlow()

    /** Makes a request for all open rooms */
    suspend fun requestOpenRooms(ownerPublicId: String?) {
        dataManager.openConversations.value = repository.getOpenRooms(ownerPublicId)
    }

    suspend fun getNetworkItems(ownerPublicId: String?) {
        if(dataManager.networkItems.value == null) {
            dataManager.networkItems.value = repository.getNetworkItems(ownerPublicId)
        }
    }

    /** Makes a request for a change of proximity of a conversation */
    suspend fun requestProximityChange(
        conversationId: String?,
        publicId: String?,
        ownerPublicId: String?,
        proximity: Float
    ) {
        withContext(Dispatchers.IO) {
            repository.patchProximity(
                conversationId = conversationId,
                publicId = publicId,
                proximity = proximity,
                ownerPublicId = ownerPublicId
            )
        }
    }

    /** Creates a new invitation */
    suspend fun inviteToConversation(
        conversationId: String?,
        ownerPublicId: String?,
        userPublicIds: List<String>?,
        message: String?,
        newName: String? = null
    ) {
        _isLoading.value = true
        repository.inviteToConversation(
            conversationId = conversationId,
            userPublicIds = userPublicIds,
            message = message,
            newName = newName,
            ownerPublicId = ownerPublicId
        ).also {
            _invitationResponse.value = it.success?.data
        }
        _isLoading.value = true
    }
}
