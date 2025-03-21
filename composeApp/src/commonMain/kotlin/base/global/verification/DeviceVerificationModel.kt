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
import kotlinx.coroutines.runBlocking
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
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod.Sas
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationRequestToDeviceEventContent
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val verificationModule = module {
    viewModelOf(::VerificationModel)
}

data class ComparisonByUserData(
    val emojis: List<Pair<String, Map<String, String>>>,
    val decimals: List<Int>,
    private val onSend: suspend (matches: Boolean) -> Unit
) {
    suspend fun send(matches: Boolean) = withContext(Dispatchers.IO) {
        onSend(matches)
    }
}

sealed class LauncherState {
    class SelfVerification(
        val methods: List<SelfVerificationMethod>
    ): LauncherState()

    data class ComparisonByUser(
        val data: ComparisonByUserData,
        val senderDeviceId: String
    ) : LauncherState()

    data object Success: LauncherState()
    data object Hidden: LauncherState()
}

class VerificationModel(
    private val authService: AuthService
): SharedModel() {
    val isLoading = MutableStateFlow(false)
    private val _launcherState = MutableStateFlow<LauncherState>(LauncherState.Hidden)
    private val _verificationResult = MutableStateFlow<Result<Unit>?>(null)

    val launcherState = _launcherState.asStateFlow()
    val verificationResult = _verificationResult.asStateFlow()

    private var hasActiveSubscribers = false

    private val supportedMethods = setOf(Sas)
    private val keyVerificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        subscribe()
        viewModelScope.coroutineContext.onCancel {
            runBlocking { clear() }
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

    suspend fun clear() {
        cancel(restart = false)
        keyVerificationScope.coroutineContext.cancelChildren()
    }

    private fun subscribeToVerificationMethods(client: MatrixClient) {
        keyVerificationScope.launch {
            client.verification.getSelfVerificationMethods().collectLatest { verification ->
                when(verification) {
                    is VerificationService.SelfVerificationMethods.AlreadyCrossSigned -> {
                        if(_launcherState.value is LauncherState.SelfVerification) {
                            _launcherState.value = LauncherState.Hidden
                        }
                    }
                    is VerificationService.SelfVerificationMethods.CrossSigningEnabled -> {
                        _launcherState.value = LauncherState.SelfVerification(
                            methods = verification.methods.distinctBy { if(it.isVerify()) "0" else it.toString() }
                        )
                    }
                    else -> {}
                }
            }
        }
        keyVerificationScope.launch {
            client.verification.activeDeviceVerification.collectLatest { deviceVerification ->
                keyVerificationScope.launch {
                    deviceVerification?.state?.collectLatest { state ->
                        onActiveState(state)
                    }
                }
            }
        }
    }

    fun cancel(restart: Boolean = true, manual: Boolean = true) {
        viewModelScope.launch {
            hasActiveSubscribers = false
            if(manual) {
                sharedDataManager.matrixClient.value?.verification?.activeDeviceVerification?.value?.cancel()
            }
            keyVerificationScope.coroutineContext.cancelChildren()
            isLoading.value = false

            _launcherState.value = if(restart) {
                subscribe()
                (sharedDataManager.matrixClient.value?.verification?.getSelfVerificationMethods()?.firstOrNull()
                        as? VerificationService.SelfVerificationMethods.CrossSigningEnabled)?.let { verification ->
                    LauncherState.SelfVerification(
                        methods = verification.methods.distinctBy { if(it.isVerify()) "0" else it.toString() }
                    )
                } ?: LauncherState.Hidden
            }else LauncherState.Hidden
        }
    }

    fun matchChallenge(matches: Boolean) {
        isLoading.value = true
        viewModelScope.launch {
            (_launcherState.value as? LauncherState.ComparisonByUser)?.data?.send(matches)

            if(!matches) {
                (_launcherState.value as? LauncherState.ComparisonByUser)?.data?.send(matches)
            }
        }
    }

    fun verifySelf(method: SelfVerificationMethod, passphrase: String) {
        if(_launcherState.value !is LauncherState.SelfVerification) return

        viewModelScope.launch {
            _verificationResult.value = null
            isLoading.value = true

            when(method) {
                is SelfVerificationMethod.AesHmacSha2RecoveryKey -> {
                    _verificationResult.value = method.verify(passphrase)
                }
                is SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase-> {
                    _verificationResult.value = method.verify(passphrase)
                }
                is SelfVerificationMethod.CrossSignedDeviceVerification -> {
                    method.createDeviceVerification().getOrNullLoggingError().let {
                        println("kostka_test, verifySelf, ${it?.theirDeviceId}")
                        if(it == null) isLoading.value = false
                    }
                }
            }
        }
    }

    private suspend fun onActiveState(state: ActiveVerificationState) {
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
                                is ActiveSasVerificationState.ComparisonByUser -> {
                                    _launcherState.value = LauncherState.ComparisonByUser(
                                        data = ComparisonByUserData(
                                            onSend = { matches ->
                                                if(matches) sasState.match() else sasState.noMatch()
                                            },
                                            decimals = sasState.decimal,
                                            emojis = sasState.emojis.mapNotNull {
                                                emojisWithTranslation[it.first]
                                            }
                                        ),
                                        senderDeviceId = state.senderDeviceId
                                    )
                                    isLoading.value = false
                                }
                                is ActiveSasVerificationState.TheirSasStart -> {
                                    println("kostka_test, sasState accept")
                                    (_launcherState.value as? LauncherState.SelfVerification)?.let {
                                        /*if(state.senderDeviceId == it.senderDeviceId) {
                                        }*/
                                        sasState.accept()
                                    }
                                }
                                is ActiveSasVerificationState.Accept -> {}
                                is ActiveSasVerificationState.OwnSasStart -> {}
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
            is ActiveVerificationState.Done -> {
                if(_launcherState.value !is LauncherState.Hidden) {
                    isLoading.value = false
                    _launcherState.value = LauncherState.Success
                }
            }
            is ActiveVerificationState.Cancel -> cancel(manual = false)
            else -> {}
        }
    }

    fun resetBootstrap(newPassphrase: String) {
        viewModelScope.launch {
            bootstrapCrossSigning(passphrase = newPassphrase)?.let { result ->
                if(result is UIA.Success<*>) {
                    finishDeviceVerification()
                }else isLoading.value = false
            }.ifNull {
                isLoading.value = false
            }
        }
    }

    private suspend fun bootstrapCrossSigning(passphrase: String): UIA<out Any?>? {
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

    private fun finishDeviceVerification() {
        viewModelScope.launch {
            sharedDataManager.matrixClient.value?.let { client ->
                client.verification.createDeviceVerificationRequest(
                    theirDeviceIds = client.key.getDeviceKeys(client.userId).firstOrNull()?.mapNotNull { key ->
                        key.deviceId.takeIf { it != client.deviceId }
                    }?.toSet().orEmpty(),
                    theirUserId = client.userId
                )
            }
        }
    }
}
