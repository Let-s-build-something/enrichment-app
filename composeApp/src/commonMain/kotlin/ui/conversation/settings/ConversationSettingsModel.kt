package ui.conversation.settings

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import data.io.matrix.room.event.ConversationRoomMember
import data.io.social.network.conversation.message.MediaIO
import data.shared.SharedModel
import io.github.vinceglb.filekit.core.PlatformFile
import io.github.vinceglb.filekit.core.extension
import korlibs.io.net.MimeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.AvatarEventContent
import net.folivo.trixnity.core.model.events.m.room.ImageInfo
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.conversation.ConversationDataManager
import ui.home.utils.NetworkItemUseCase
import ui.login.AUGMY_HOME_SERVER

val conversationSettingsModule = module {
    factory { ConversationSettingsRepository(get(), get(), get(), get(), get()) }
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
    }

    private val _isLoading = MutableStateFlow(false)
    private val _changeAvatarResponse = MutableSharedFlow<Result<EventId>?>()

    /** Detailed information about this conversation */
    val conversation = MutableStateFlow(dataManager.conversations.value[conversationId])
    val isLoading = _isLoading.asStateFlow()
    val changeAvatarResponse = _changeAvatarResponse.asSharedFlow()

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

    fun requestAliasChange(alias: String) {
        viewModelScope.launch {
            sharedDataManager.matrixClient.value?.api?.room?.setRoomAlias(
                roomId = RoomId(conversationId),
                roomAliasId = RoomAliasId(alias)
            )
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

        _isLoading.value = true
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

            _changeAvatarResponse.emit(
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
                    if(res?.getOrNull() != null) {
                        dataManager.conversations.update { prev ->
                            prev.apply {
                                this[conversationId]?.copy(
                                    summary = this[conversationId]?.summary?.copy(avatar = media)
                                )?.let {
                                    set(conversationId, it)
                                    conversation.value = it
                                }
                            }
                        }
                    }
                }
            )
            _isLoading.value = false
        }
    }
}
