package base.global.verification

import androidx.lifecycle.viewModelScope
import data.shared.SharedModel
import korlibs.io.util.getOrNullLoggingError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.ActiveSasVerificationMethod
import net.folivo.trixnity.client.verification.ActiveSasVerificationState
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.client.verification.SelfVerificationMethod
import net.folivo.trixnity.client.verification.VerificationService
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.key.verification.SasHash
import net.folivo.trixnity.core.model.events.m.key.verification.SasKeyAgreementProtocol.Curve25519HkdfSha256
import net.folivo.trixnity.core.model.events.m.key.verification.SasMessageAuthenticationCode.HkdfHmacSha256
import net.folivo.trixnity.core.model.events.m.key.verification.SasMessageAuthenticationCode.HkdfHmacSha256V2
import net.folivo.trixnity.core.model.events.m.key.verification.SasMethod
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationRequestToDeviceEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationStartEventContent
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val verificationModule = module {
    viewModelOf(::VerificationModel)
}

class VerificationModel: SharedModel() {
    val isLoading = MutableStateFlow(false)
    private val _verificationMethods = MutableStateFlow<Set<SelfVerificationMethod>>(setOf())
    private val _comparisonByUser = MutableStateFlow<ActiveSasVerificationState.ComparisonByUser?>(null)

    val verificationMethods = _verificationMethods.asStateFlow()
    val comparisonByUser = _comparisonByUser.asStateFlow()

    private val keyVerificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        viewModelScope.launch {
            sharedDataManager.matrixClient.collectLatest {
                it?.let { client ->
                    keyVerificationScope.coroutineContext.cancelChildren()
                    subscribeToVerificationMethods(client = client)
                }
            }
        }
    }

    fun matchChallenge(matches: Boolean) {
        viewModelScope.launch {
            if(matches) {
                comparisonByUser.value?.match()
            }else comparisonByUser.value?.noMatch()
        }
    }

    fun verify(passphrase: String?) {
        viewModelScope.launch {
            verificationMethods.value.forEach { method ->
                isLoading.value = true
                when(method) {
                    is SelfVerificationMethod.AesHmacSha2RecoveryKey -> {
                        if(passphrase != null) method.verify(passphrase)
                    }
                    is SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase -> {
                        if(passphrase != null) method.verify(passphrase)
                    }
                    is SelfVerificationMethod.CrossSignedDeviceVerification -> {
                        method.createDeviceVerification().getOrNullLoggingError().let { res ->
                            if(res != null) {
                                res.state.collectLatest {  state ->
                                    when(state) {
                                        is ActiveVerificationState.AcceptedByOtherDevice,
                                        is ActiveVerificationState.Done -> {
                                            isLoading.value = false
                                        }
                                        is ActiveVerificationState.OwnRequest -> {}
                                        is ActiveVerificationState.Cancel -> isLoading.value = false
                                        is ActiveVerificationState.Ready -> {
                                            println("kostka_test, ActiveVerificationState.Ready, methods: ${state.methods}")
                                            // TODO state.start(method)
                                        }
                                        else -> {}
                                    }
                                    println("kostka_test, verification state: $state")
                                }
                            }else isLoading.value = false
                        }
                    }
                }
            }
        }
    }

    private fun subscribeToVerificationMethods(client: MatrixClient) {
        keyVerificationScope.launch {
            client.verification.getSelfVerificationMethods().collectLatest {
                if(it is VerificationService.SelfVerificationMethods.CrossSigningEnabled) {
                    _verificationMethods.value = it.methods
                }else _verificationMethods.value = setOf()
                println("kostka_test, crossSigning method: $it")
            }

            client.verification.activeDeviceVerification.collectLatest { deviceVerification ->
                deviceVerification?.state?.collectLatest { state ->
                    println("kostka_test, state: $state")
                    when(state) {
                        is ActiveVerificationState.TheirRequest -> {
                            when(val content = state.content) {
                                is VerificationRequestToDeviceEventContent -> {
                                    content.methods.forEach { method ->
                                        when(method) {
                                            VerificationMethod.Sas -> {
                                                println("kostka_test, marking Sas as ready")
                                                state.ready()
                                            }
                                            is VerificationMethod.Unknown -> {
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is ActiveVerificationState.Start -> {
                            when(val method = state.method) {
                                is ActiveSasVerificationMethod -> {
                                    method.state.collectLatest { sasState ->
                                        when(sasState) {
                                            is ActiveSasVerificationState.Accept -> {}
                                            is ActiveSasVerificationState.ComparisonByUser -> {
                                                _comparisonByUser.value = sasState
                                                println("kostka_test, sasState match")
                                            }
                                            is ActiveSasVerificationState.OwnSasStart -> {

                                            }
                                            is ActiveSasVerificationState.TheirSasStart -> {
                                                println("kostka_test, sasState accept")
                                                sasState.accept()
                                            }
                                            is ActiveSasVerificationState.WaitForKeys -> {}
                                            ActiveSasVerificationState.WaitForMacs -> {}
                                        }
                                        println("kostka_test, sasState: $sasState")
                                    }
                                }
                            }
                        }
                        is ActiveVerificationState.Ready -> {
                            if(state.methods.contains(VerificationMethod.Sas)) {
                                println("kostka_test, NOT starting Sas")
                                //state.start(VerificationMethod.Sas)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun acceptSasVerification(
        client: MatrixClient,
        transactionId: String?
    ) {
        sharedDataManager.currentUser.value?.matrixUserId?.let { userId ->
            sharedDataManager.localSettings.value?.deviceId?.let { deviceId ->
                val data = VerificationStartEventContent.SasStartEventContent(
                    fromDevice = deviceId,
                    relatesTo = null,
                    hashes = setOf(SasHash.Sha256),
                    keyAgreementProtocols = setOf(Curve25519HkdfSha256),
                    messageAuthenticationCodes = setOf(HkdfHmacSha256, HkdfHmacSha256V2),
                    shortAuthenticationString = setOf(SasMethod.Decimal, SasMethod.Emoji),
                    transactionId = transactionId
                )

                keyVerificationScope.launch {
                    client.api.user.sendToDevice(
                        events = mapOf(UserId(userId) to mapOf(deviceId to data))
                    )
                }
            }
        }
    }
}
