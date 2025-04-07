package ui.conversation.settings

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import data.io.matrix.room.event.ConversationRoomMember
import data.shared.SharedModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.conversation.ConversationDataManager
import ui.home.utils.NetworkItemUseCase
import ui.login.AUGMY_HOME_SERVER

val conversationSettingsModule = module {
    factory { ConversationSettingsRepository(get(), get(), get()) }
    viewModelOf(::ConversationSettingsModel)
}

class ConversationSettingsModel(
    private val conversationId: String,
    private val networkItemUseCase: NetworkItemUseCase,
    repository: ConversationSettingsRepository,
    dataManager: ConversationDataManager
): SharedModel() {
    companion object {
        const val MAX_MEMBERS_COUNT = 8
        const val PAGE_ITEM_COUNT = 20
    }

    /** Detailed information about this conversation */
    val conversation = dataManager.conversations.map { it[conversationId] }

    val members: Flow<PagingData<ConversationRoomMember>> = repository.getMembersListFlow(
        config = PagingConfig(
            pageSize = PAGE_ITEM_COUNT,
            enablePlaceholders = true
        ),
        homeserver = {
            currentUser.value?.matrixHomeserver ?: AUGMY_HOME_SERVER
        },
        conversationId = conversationId
    ).flow.cachedIn(viewModelScope)

    /** Removes a member out of a conversation */
    fun kickMember(memberId: String) {
        viewModelScope.launch {
            sharedDataManager.matrixClient.value?.api?.room?.kickUser(
                roomId = RoomId(conversationId),
                userId = UserId(memberId)
            )
        }
    }

    fun ignoreMember(memberId: String) {
        viewModelScope.launch {
            sharedDataManager.matrixClient.value?.api?.room?.kickUser(
                roomId = RoomId(conversationId),
                userId = UserId(memberId)
            )
        }
    }

    /** Makes a request for a change of proximity of a conversation */
    fun requestProximityChange(
        publicId: String?,
        proximity: Float,
        onOperationDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            networkItemUseCase.requestProximityChange(
                conversationId = conversationId,
                publicId = publicId,
                proximity = proximity,
                ownerPublicId = matrixUserId
            )
            onOperationDone()
        }
    }
}