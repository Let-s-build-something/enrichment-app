package ui.conversation.message.user_verification

import androidx.lifecycle.viewModelScope
import base.global.verification.ComparisonByUserData
import base.global.verification.emojisWithTranslation
import data.io.base.AppPing
import data.io.base.AppPingType
import data.shared.SharedModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.ActiveSasVerificationMethod
import net.folivo.trixnity.client.verification.ActiveSasVerificationState
import net.folivo.trixnity.client.verification.ActiveUserVerification
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.VerificationRequest
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val userVerificationModule = module {
    viewModelOf(::UserVerificationModel)
}

class UserVerificationModel(
    private val conversationId: String
): SharedModel() {

    private val supportedVerificationMethods = setOf(VerificationMethod.Sas)
    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Hidden)
    private var activeUserVerification: ActiveUserVerification? = null
    private var userVerificationState: ActiveVerificationState? = null
    private val _isLoading = MutableStateFlow(false)

    val verificationState = _verificationState.asStateFlow()
    val isLoading = _isLoading.asStateFlow()

    fun matchChallenge(matches: Boolean) {
        (_verificationState.value as? VerificationState.ComparisonByUser)?.data?.let { data ->
            viewModelScope.launch {
                _isLoading.value = true
                data.send(matches)
            }
        }
    }


    /**
     * Preparation of user verification.
     * Attempts to find the verification and update the message locally if no verification was found
     */
    fun getUserVerification(eventId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            matrixClient?.verification?.getActiveUserVerification(
                roomId = RoomId(conversationId),
                eventId = EventId(eventId)
            ).also {
                if(it == null) {
                    cancel()
                }else {
                    activeUserVerification = it
                    _isLoading.value = false
                    it.state.collectLatest { state ->
                        when(state) {
                            is ActiveVerificationState.OwnRequest -> {
                                _isLoading.value = true
                                _verificationState.value = VerificationState.Awaiting
                            }
                            is ActiveVerificationState.TheirRequest -> {
                                (state.content as? VerificationRequest)?.let { content ->
                                    _verificationState.value = VerificationState.TheirRequest(methods = content.methods)
                                }
                            }
                            is ActiveVerificationState.Start -> {
                                _verificationState.value = VerificationState.Start
                                when(val method = state.method) {
                                    is ActiveSasVerificationMethod -> {
                                        method.state.collectLatest { sasState ->
                                            when(sasState) {
                                                is ActiveSasVerificationState.ComparisonByUser -> {
                                                    _verificationState.value = VerificationState.ComparisonByUser(
                                                        data = ComparisonByUserData(
                                                            onSend = { matches ->
                                                                if (matches) sasState.match() else sasState.noMatch()
                                                            },
                                                            decimals = sasState.decimal,
                                                            emojis = sasState.emojis.mapNotNull {
                                                                emojisWithTranslation[it.first]
                                                            }
                                                        )
                                                    )
                                                    _isLoading.value = false
                                                }
                                                is ActiveSasVerificationState.TheirSasStart -> {
                                                    if(state.senderUserId.full != matrixUserId) {
                                                        sasState.accept()
                                                    }
                                                }
                                                is ActiveSasVerificationState.Accept -> {}
                                                is ActiveSasVerificationState.OwnSasStart -> {}
                                                is ActiveSasVerificationState.WaitForKeys -> {}
                                                ActiveSasVerificationState.WaitForMacs -> {}
                                            }
                                        }
                                    }
                                }
                            }
                            is ActiveVerificationState.Ready -> {
                                state.methods.firstOrNull { supportedVerificationMethods.contains(it) }?.let { method ->
                                    //state.start(method)
                                    _verificationState.value = VerificationState.Ready
                                }
                            }
                            is ActiveVerificationState.Done, is ActiveVerificationState.Undefined -> {
                                _isLoading.value = false
                                _verificationState.value = VerificationState.Success
                            }
                            is ActiveVerificationState.Cancel -> {
                                _isLoading.value = false
                                _verificationState.value = VerificationState.Canceled
                            }
                            else -> {}
                        }

                        userVerificationState = state
                    }
                }
                _isLoading.value = false
            }
        }
    }

    fun cancel() {
        viewModelScope.launch {
            activeUserVerification?.cancel()
            _verificationState.value = VerificationState.Canceled
            sharedDataManager.pingStream.update { prev ->
                prev.plus(
                    AppPing(
                        type = AppPingType.Conversation,
                        identifier = conversationId
                    )
                )
            }
        }
    }

    /** user-initiated request to follow up */
    fun confirmUserVerification() {
        if(userVerificationState == null) return

        _isLoading.value = true
        viewModelScope.launch {
            (userVerificationState as? ActiveVerificationState.TheirRequest)?.ready()
            _isLoading.value = false
        }
    }
}
