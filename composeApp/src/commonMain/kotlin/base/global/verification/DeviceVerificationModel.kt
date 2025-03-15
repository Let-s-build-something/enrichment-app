package base.global.verification

import androidx.lifecycle.viewModelScope
import augmy.interactive.shared.ext.ifNull
import data.shared.SharedModel
import data.shared.auth.AuthService
import korlibs.io.async.onCancel
import korlibs.io.util.getOrNullLoggingError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.ActiveSasVerificationMethod
import net.folivo.trixnity.client.verification.ActiveSasVerificationState
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.client.verification.SelfVerificationMethod
import net.folivo.trixnity.client.verification.VerificationService
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.key.verification.SasHash
import net.folivo.trixnity.core.model.events.m.key.verification.SasKeyAgreementProtocol.Curve25519HkdfSha256
import net.folivo.trixnity.core.model.events.m.key.verification.SasMessageAuthenticationCode.HkdfHmacSha256
import net.folivo.trixnity.core.model.events.m.key.verification.SasMessageAuthenticationCode.HkdfHmacSha256V2
import net.folivo.trixnity.core.model.events.m.key.verification.SasMethod
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod.Sas
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationRequestToDeviceEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationStartEventContent
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val verificationModule = module {
    viewModelOf(::VerificationModel)
}

data class ComparisonByUser(
    val emojis: List<Pair<String, Map<String, String>>>,
    val decimals: List<Int>,
    private val onSend: suspend (matches: Boolean) -> Unit
) {
    suspend fun send(matches: Boolean) = withContext(Dispatchers.IO) {
        onSend(matches)
    }
}

class VerificationModel(
    private val authService: AuthService
): SharedModel() {
    val isLoading = MutableStateFlow(false)
    private val _showLauncher = MutableStateFlow(false)
    private val _comparisonByUser = MutableStateFlow<ComparisonByUser?>(null)
    private val _verificationResult = MutableStateFlow<Result<Unit>?>(null)
    private val verificationMethods = MutableStateFlow<Set<SelfVerificationMethod>>(setOf())

    val showLauncher = _showLauncher.asStateFlow()
    val comparisonByUser = _comparisonByUser.asStateFlow()
    val verificationResult = _verificationResult.asStateFlow()

    private var hasActiveSubscribers = false

    private val supportedMethods = setOf(Sas)
    private val keyVerificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        subscribe()
        viewModelScope.coroutineContext.onCancel {
            clear()
        }
    }

    private fun subscribe() {
        hasActiveSubscribers = true
        keyVerificationScope.coroutineContext.cancelChildren()

        keyVerificationScope.launch {
            sharedDataManager.matrixClient.collectLatest {
                it?.let { client ->
                    subscribeToVerificationMethods(client = client)
                }
            }
        }
    }

    fun clear() {
        cancel(restart = false)
        keyVerificationScope.coroutineContext.cancelChildren()
        _showLauncher.value = false
        _comparisonByUser.value = null
    }

    fun cancel(restart: Boolean = true, manual: Boolean = true) {
        hasActiveSubscribers = false
        viewModelScope.launch {
            sharedDataManager.matrixClient.value?.verification?.activeDeviceVerification?.value?.cancel()
        }
        keyVerificationScope.coroutineContext.cancelChildren()
        _comparisonByUser.value = null
        isLoading.value = false

        if(restart) subscribe()
    }

    fun matchChallenge(matches: Boolean) {
        viewModelScope.launch {
            if(matches) {
                comparisonByUser.value?.send(matches)
            }else {
                comparisonByUser.value?.send(matches)
                _comparisonByUser.value = null
            }
        }
    }

    private suspend fun bootstrapCrossSigning(passphrase: String): UIA<out Any?>? {
        println("kostka_test, starting bootstrapCrossSigning")
        return sharedDataManager.matrixClient.value?.key?.bootstrapCrossSigningFromPassphrase(
            passphrase = passphrase
        )?.result?.getOrThrow()?.let { result ->
            println("kostka_test, bootstrap result: $result")
            processBootstrapResult(result)
        }
    }

    private suspend fun <T>processBootstrapResult(result: UIA<T>, count: Int = 0): UIA<out Any?> {
        println("kostka_test, processBootstrapResult: $result")
        return when(result) {
            is UIA.Step -> {
                authService.createLoginRequest()?.let { request ->
                    println("kostka_test, auth request: $request, count: $count")
                    if(count < 3) {
                        result.authenticate(request).getOrNullLoggingError()?.let { res ->
                            println("kostka_test, auth res: $res")
                            processBootstrapResult(result = res, count = count + 1)
                        }
                    }else result
                } ?: result
            }
            is UIA.Error<*> -> {
                authService.createLoginRequest()?.let { request ->
                    if(count < 3) {
                        result.authenticate(request).getOrNullLoggingError()?.let { res ->
                            processBootstrapResult(result = res, count = count + 1)
                        }
                    }else result
                } ?: result
            }
            is UIA.Success<*> -> result
        }
    }

    fun verify(state: VerificationState, passphrase: String) {
        viewModelScope.launch {
            _verificationResult.value = null
            isLoading.value = true

            when(state) {
                VerificationState.ComparisonByUser -> {}
                VerificationState.OtherDevice -> {
                    (verificationMethods.value.find {
                        it is SelfVerificationMethod.CrossSignedDeviceVerification
                    } as? SelfVerificationMethod.CrossSignedDeviceVerification)?.let { method ->
                        method.createDeviceVerification().getOrNullLoggingError().let {
                            if(it == null) isLoading.value = false
                        }
                        /*_verificationResult.value = method.verify(passphrase).also {
                            println("kostka_test, verify.res: $it")
                            if(it.isSuccess) finishDeviceVerification()
                        }
                        isLoading.value = false*/
                    }.ifNull { isLoading.value = false }
                }
                VerificationState.Passphrase -> {
                    bootstrapCrossSigning(passphrase = passphrase)?.let { result ->
                        if(result is UIA.Success<*>) {
                            finishDeviceVerification()
                        }else isLoading.value = false
                    }.ifNull {
                        isLoading.value = false
                    }
                }
            }
        }
    }

    private fun finishDeviceVerification() {
        viewModelScope.launch {
            temporaryCheckTrustLevel()
            sharedDataManager.matrixClient.value?.let { client ->
                println("kostka_test, finishDeviceVerification, verification methods ${client.verification.getSelfVerificationMethods().firstOrNull()}")
                client.verification.createDeviceVerificationRequest(
                    theirDeviceIds = client.key.getDeviceKeys(client.userId).firstOrNull()?.mapNotNull { key ->
                        key.deviceId.takeIf { it != client.deviceId }
                    }?.toSet().orEmpty().also {
                        println("kostka_test, theirDeviceIds: $it")
                    },
                    theirUserId = client.userId
                )
            }
        }
    }

    private suspend fun temporaryCheckTrustLevel() {
        sharedDataManager.matrixClient.value?.let { client ->
            sharedDataManager.currentUser.value?.matrixUserId?.let { userId ->
                sharedDataManager.localSettings.value?.deviceId?.let { deviceId ->
                    println("kostka_test, current trustLevel: ${
                        client.key.getTrustLevel(
                            userId = UserId(userId),
                            deviceId = deviceId
                        ).firstOrNull()
                    }")
                }
            }
        }
    }

    private fun subscribeToVerificationMethods(client: MatrixClient) {
        keyVerificationScope.launch {
            client.verification.getSelfVerificationMethods().collectLatest { it ->
                _showLauncher.value = it !is VerificationService.SelfVerificationMethods.AlreadyCrossSigned
                if(it is VerificationService.SelfVerificationMethods.CrossSigningEnabled) {
                    verificationMethods.value = it.methods
                    println("kostka_test, crossSigning methods: ${it.methods.map { it::class.simpleName }}")
                    temporaryCheckTrustLevel()
                }
            }
        }
        keyVerificationScope.launch {
            client.verification.activeDeviceVerification.collectLatest { deviceVerification ->
                keyVerificationScope.launch {
                    deviceVerification?.state?.collectLatest { state ->
                        println("kostka_test, state: $state")
                        when(state) {
                            is ActiveVerificationState.TheirRequest -> {
                                when(val content = state.content) {
                                    is VerificationRequestToDeviceEventContent -> {
                                        content.methods.forEach { method ->
                                            when(method) {
                                                Sas -> {
                                                    println("kostka_test, marking Sas as ready")
                                                    state.ready()
                                                    /*startSasVerification(
                                                        client = client,
                                                        transactionId = content.transactionId
                                                    )*/
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
                                                    _comparisonByUser.value = ComparisonByUser(
                                                        onSend = { matches ->
                                                            if(matches) sasState.match() else sasState.noMatch()
                                                        },
                                                        decimals = sasState.decimal,
                                                        emojis = sasState.emojis.mapNotNull {
                                                            emojisWithTranslation[it.first]
                                                        }
                                                    )
                                                    _showLauncher.value = true
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
                                state.methods.firstOrNull { supportedMethods.contains(it) }?.let { method ->
                                    println("kostka_test, NOT starting $method")
                                    //state.start(method)
                                }
                            }
                            is ActiveVerificationState.Cancel -> cancel(manual = false)
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun startSasVerification(
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
