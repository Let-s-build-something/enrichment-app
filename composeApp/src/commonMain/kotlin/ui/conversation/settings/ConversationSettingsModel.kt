package ui.conversation.settings

import androidx.lifecycle.viewModelScope
import data.io.matrix.room.event.ConversationRoomMember
import data.shared.SharedModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.conversation.ConversationDataManager
import ui.home.utils.NetworkItemUseCase

val conversationSettingsModule = module {
    factory { ConversationSettingsRepository(get()) }
    viewModelOf(::ConversationSettingsModel)
}

class ConversationSettingsModel(
    private val conversationId: String,
    private val networkItemUseCase: NetworkItemUseCase,
    private val repository: ConversationSettingsRepository,
    private val dataManager: ConversationDataManager
): SharedModel() {
    private val _members = MutableStateFlow(listOf<ConversationRoomMember>())

    /** Detailed information about this conversation */
    val conversation = dataManager.conversations.map { it[conversationId] }
    val members = _members.asStateFlow()

    init {
        if(_members.value.isEmpty()) {
            viewModelScope.launch {
                _members.value = repository.getMembers(conversationId)
            }
        }
    }

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

    fun paginateMembers(enable: Boolean) {
        // TODO
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