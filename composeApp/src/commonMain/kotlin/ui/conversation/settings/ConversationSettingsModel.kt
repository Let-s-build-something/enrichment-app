package ui.conversation.settings

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import base.global.verification.ComparisonByUserData
import data.io.base.BaseResponse
import data.io.matrix.room.event.ConversationRoomMember
import data.io.social.network.conversation.message.MediaIO
import data.io.user.NetworkItemIO
import data.shared.SharedModel
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import korlibs.io.net.MimeType
import korlibs.io.util.getOrNullLoggingError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.ActiveUserVerification
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.AvatarEventContent
import net.folivo.trixnity.core.model.events.m.room.ImageInfo
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.conversation.ConversationDataManager
import ui.home.utils.NetworkItemUseCase

val conversationSettingsModule = module {
    factory {
        ConversationSettingsRepository(get(), get(), get(), get(), get(), get(), get(), get())
    }
    viewModelOf(::ConversationSettingsModel)
}

class ConversationSettingsModel(
    private val conversationId: String,
    private val networkItemUseCase: NetworkItemUseCase,
    private val repository: ConversationSettingsRepository,
    private val dataManager: ConversationDataManager
): SharedModel() {
    companion object {
        const val MAX_MEMBERS_COUNT = 8
        const val PAGE_ITEM_COUNT = 20

        fun ActiveVerificationState.isFinished() = this is ActiveVerificationState.Cancel
                || this is ActiveVerificationState.Done
                || this is ActiveVerificationState.WaitForDone
    }

    sealed class ChangeType(open val state: BaseResponse<*>) {
        data class Avatar(override val state: BaseResponse<*>): ChangeType(state)
        data class Name(override val state: BaseResponse<*>): ChangeType(state)
        data class Leave(override val state: BaseResponse<*>): ChangeType(state)
        data class InviteMember(override val state: BaseResponse<*>): ChangeType(state)
        data class VerifyMember(
            override val state: BaseResponse<*>,
            val data: ComparisonByUserData? = null
        ): ChangeType(state)
    }

    private val _ongoingChange = MutableStateFlow<ChangeType?>(null)
    private val _selectedUser = MutableStateFlow<NetworkItemIO?>(null)
    private val _verifications = MutableStateFlow<HashMap<String, ActiveUserVerification?>>(hashMapOf())

    /** Detailed information about this conversation */
    val conversation = dataManager.conversations.map { it.second[conversationId] }
    val ongoingChange = _ongoingChange.asStateFlow()
    val selectedUser = _selectedUser.asStateFlow()
    val verifications = _verifications.asStateFlow()

    val members: Flow<PagingData<ConversationRoomMember>> = repository.getMembersListFlow(
        config = PagingConfig(
            pageSize = PAGE_ITEM_COUNT,
            enablePlaceholders = true
        ),
        homeserver = { homeserver },
        ignoreUserId = matrixUserId,
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

    fun requestRoomNameChange(roomName: CharSequence) {
        _ongoingChange.value = ChangeType.Name(BaseResponse.Loading)
        viewModelScope.launch {
            sharedDataManager.matrixClient.value?.api?.room?.sendStateEvent(
                roomId = RoomId(conversationId),
                eventContent = NameEventContent(name = roomName.toString())
            ).also { res ->
                _ongoingChange.value = ChangeType.Name(
                    if(res?.getOrNull() != null) {
                        dataManager.updateConversations { prev ->
                            prev.apply {
                                this[conversationId]?.copy(
                                    summary = this[conversationId]?.summary?.copy(canonicalAlias = roomName.toString())
                                )?.let {
                                    set(conversationId, it)
                                    repository.updateRoom(it)
                                }
                            }
                        }
                        BaseResponse.Success(null)
                    }else BaseResponse.Error()
                )
            }
        }
    }

    fun leaveRoom(reason: CharSequence) {
        _ongoingChange.value = ChangeType.Leave(BaseResponse.Loading)
        viewModelScope.launch {
            sharedDataManager.matrixClient.value?.api?.room?.leaveRoom(
                roomId = RoomId(conversationId),
                reason = reason.takeIf { it.isNotBlank() }?.toString()
            ).also { res ->
                _ongoingChange.value = ChangeType.Leave(
                    if(res?.getOrNull() != null) {
                        dataManager.updateConversations { prev ->
                            prev.apply {
                                remove(conversationId)
                                repository.removeRoom(
                                    conversationId = conversationId,
                                    ownerPublicId = matrixUserId
                                )
                            }
                        }
                        BaseResponse.Success(null)
                    }else BaseResponse.Error()
                )
            }
        }
    }

    fun selectUser(userId: String?) {
        viewModelScope.launch {
            _selectedUser.value = if(userId == null) null else repository.getUser(userId, matrixUserId)
        }
    }

    suspend fun getActiveVerification(
        userId: String,
        unfinishedOnly: Boolean = false
    ) = repository.getPendingVerifications(
        senderUserId = matrixUserId
    ).mapNotNull { message ->
        if(message.verification?.to == userId) {
            matrixClient?.verification?.getActiveUserVerification(
                roomId = RoomId(conversationId),
                eventId = EventId(message.id)
            )?.takeIf { !unfinishedOnly || it.state.value.isFinished() == false }
        } else null
    }

    /** Check for a given user's verification state */
    fun checkVerificationState(userId: String?) {
        if(userId == null) return
        viewModelScope.launch {
            _verifications.update {
                it.apply {
                    set(userId, getActiveVerification(userId).firstOrNull())
                }
            }
        }
    }

    fun verifyUser(userId: String?) {
        if(userId == null) return
        _ongoingChange.value = ChangeType.VerifyMember(BaseResponse.Loading)
        viewModelScope.launch {
            if(getActiveVerification(userId = userId, unfinishedOnly = true).isEmpty()) {
                matrixClient?.verification?.createUserVerificationRequest(UserId(userId))
                    ?.getOrNullLoggingError()
                    ?.roomId
                    .let { roomId ->
                        checkVerificationState(userId)
                        _ongoingChange.value = ChangeType.VerifyMember(
                            if(roomId != null) BaseResponse.Success(roomId.full) else BaseResponse.Error()
                        )
                    }
            }
        }
    }

    fun inviteMembers(userId: String) {
        _ongoingChange.value = ChangeType.InviteMember(BaseResponse.Loading)
        viewModelScope.launch {
            sharedDataManager.matrixClient.value?.api?.room?.inviteUser(
                roomId = RoomId(conversationId),
                userId = UserId(userId)
            ).also { res ->
                _ongoingChange.value = ChangeType.InviteMember(
                    if(res?.getOrNull() != null) {
                        dataManager.updateConversations { prev ->
                            prev.apply {
                                // TODO change locally
                            }
                        }
                        BaseResponse.Success(null)
                    }else BaseResponse.Error()
                )
            }
        }
    }

    /**
     * If [file] is not null, it is firstly uploaded to the server and then attempted to be used as a room avatar.
     */
    fun requestAvatarChange(
        file: PlatformFile?,
        url: String?
    ) {
        if(sharedDataManager.matrixClient.value == null) return

        _ongoingChange.value = ChangeType.Avatar(BaseResponse.Loading)
        viewModelScope.launch {
            var media = MediaIO(url = url)
            sharedDataManager.currentUser.value?.matrixHomeserver?.let { homeserver ->
                if(file != null) {
                    repository.uploadMedia(
                        mediaByteArray = file.readBytes(),
                        fileName = file.name,
                        mimetype = MimeType.getByExtension(file.extension).mime,
                        homeserver = homeserver
                    )?.success?.data?.contentUri?.let {
                        media = MediaIO(
                            mimetype = media.mimetype,
                            size = media.size,
                            url = it
                        )
                    }
                }
            }

            sharedDataManager.matrixClient.value?.api?.room?.sendStateEvent(
                roomId = RoomId(conversationId),
                eventContent = AvatarEventContent(
                    url = media.url,
                    info = ImageInfo(
                        mimeType = media.mimetype,
                        size = media.size
                    )
                )
            ).also { res ->
                _ongoingChange.value = ChangeType.Avatar(
                    if(res?.getOrNull() != null) {
                        dataManager.updateConversations { prev ->
                            prev.apply {
                                this[conversationId]?.copy(
                                    summary = this[conversationId]?.summary?.copy(avatar = media)
                                )?.let {
                                    set(conversationId, it)
                                    repository.updateRoom(it)
                                }
                            }
                        }
                        BaseResponse.Success(null)
                    }else BaseResponse.Error()
                )
            }
        }
    }
}
