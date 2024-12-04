package ui.conversation

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import components.pull_refresh.RefreshableViewModel
import data.io.social.network.conversation.ConversationMessageIO
import data.io.social.network.conversation.NetworkConversationIO
import data.shared.SharedViewModel
import data.shared.fromByteArrayToData
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ui.conversation.ConversationRepository.Companion.demoConversationDetail

internal val conversationModule = module {
    factory { ConversationRepository(get()) }
    factory { ConversationViewModel(get<ConversationRepository>(), get()) }
    viewModelOf(::ConversationViewModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class ConversationViewModel(
    private val repository: ConversationRepository,
    private val conversationId: String
): SharedViewModel(), RefreshableViewModel {

    override val isRefreshing = MutableStateFlow(false)
    override var lastRefreshTimeMillis = 0L

    override suspend fun onDataRequest(isSpecial: Boolean, isPullRefresh: Boolean) {}

    /** flow of current messages */
    val conversationMessages: Flow<PagingData<ConversationMessageIO>> = repository.getMessagesListFlow(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = true,
            initialLoadSize = 20
        ),
        conversationId = conversationId
    ).flow.cachedIn(viewModelScope)

    private val _conversationDetail = MutableStateFlow<NetworkConversationIO?>(null)

    /** Detailed information about this conversation */
    val conversationDetail = _conversationDetail.asStateFlow()

    /** Last saved message relevant to this conversation */
    var savedMessage: String = settings.getStringOrNull("KEY_LAST_MESSAGE_$conversationId") ?: ""
        set(value) {
            field = value
            settings.putString("KEY_LAST_MESSAGE_$conversationId", value)
        }

    init {
        if(conversationId.isNotBlank() && _conversationDetail.value?.publicId != conversationId) {
            viewModelScope.launch {
                repository.getConversationDetail(conversationId = conversationId).success?.data?.let { data ->
                    _conversationDetail.value = data
                }
            }
        }
        // TODO remove demo data
        _conversationDetail.value = demoConversationDetail
    }


    /** Makes a request to change user's profile picture */
    fun requestPictureUpload(
        mediaByteArray: ByteArray?,
        fileName: String
    ) {
        if(mediaByteArray == null) return

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val previousUrl = "${try { firebaseUser.value?.photoURL }catch (e: NotImplementedError) { null }}"

                val previousFileSuffix = """.+profile-picture(\.\w*).+""".toRegex()
                    .matchEntire(previousUrl)
                    ?.groupValues
                    ?.getOrNull(1)

                uploadPictureStorage(
                    byteArray = mediaByteArray,
                    fileName = fileName,
                    previousFileSuffix = previousFileSuffix
                )
            }
        }
    }

    /** @return if true, it was successful, if false, it failed */
    private suspend fun uploadPictureStorage(
        byteArray: ByteArray,
        fileName: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val fileSuffix = ".${fileName.split(".").lastOrNull()}"

            val reference = Firebase.storage.reference.child(
                "${firebaseUser.value?.uid}/profile-picture$fileSuffix"
            )

            reference.putData(fromByteArrayToData(byteArray))
            val newUrl = reference.getDownloadUrl()

            if(newUrl.isNotBlank()) { }
                requestPictureChange(newUrl)
                true
            }else false
        }
    }
}
