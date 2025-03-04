package data.shared.crypto.verification

import data.shared.crypto.EncryptionServiceRepository
import data.shared.crypto.KeyTrustService
import data.shared.crypto.OlmCryptoStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.ClientEvent.ToDeviceEvent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent.Code
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent.Code.Accepted
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent.Code.Timeout
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent.Code.UnexpectedMessage
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent.Code.UnknownMethod
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationDoneEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationReadyEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationRequest
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationRequestToDeviceEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationStartEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationStartEventContent.SasStartEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationStep
import net.folivo.trixnity.crypto.olm.DecryptedOlmEventContainer
import net.folivo.trixnity.crypto.olm.OlmDecrypter
import net.folivo.trixnity.crypto.olm.OlmEncryptionService

private val log = KotlinLogging.logger {}

interface ActiveDeviceVerification : ActiveVerification

class ActiveDeviceVerificationImpl(
    request: VerificationRequestToDeviceEventContent,
    requestIsOurs: Boolean,
    ownUserId: UserId,
    ownDeviceId: String,
    theirUserId: UserId,
    json: Json,
    theirDeviceId: String? = null,
    private val theirDeviceIds: Set<String> = setOf(),
    supportedMethods: Set<VerificationMethod>,
    private val olmDecrypter: OlmDecrypter,
    private val repository: EncryptionServiceRepository,
    private val olmEncryptionService: OlmEncryptionService,
    keyTrust: KeyTrustService,
    keyStore: OlmCryptoStore,
    private val clock: Clock,
) : ActiveDeviceVerification, ActiveVerificationImpl(
    request,
    requestIsOurs,
    ownUserId,
    ownDeviceId,
    theirUserId,
    theirDeviceId,
    request.timestamp,
    supportedMethods,
    null,
    request.transactionId,
    keyStore,
    keyTrust,
    json,
) {
    override suspend fun sendVerificationStep(step: VerificationStep) {
        log.debug { "send verification step $step" }
        val theirDeviceId = this.theirDeviceId
        val theirDeviceIds =
            if (theirDeviceId == null && step is VerificationCancelEventContent) theirDeviceIds
            else setOfNotNull(theirDeviceId)

        if (theirDeviceIds.isNotEmpty())
            repository.sendToDevice(mapOf(theirUserId to theirDeviceIds.associateWith {
                olmEncryptionService.encryptOlm(step, theirUserId, it).getOrNull()
                    ?: step
            })).getOrThrow()
    }

    override suspend fun lifecycle() {
        val unsubscribeHandleOlmDecryptedVerificationRequestEvents =
            olmDecrypter.subscribe(::handleOlmDecryptedVerificationRequestEvents)
        try {
            // we do this, because otherwise the timeline job could run infinite, when no new timeline event arrives
            while (isVerificationRequestActive(timestamp, clock, state.value)) {
                delay(500)
            }
            if (isVerificationTimedOut(timestamp, clock, state.value)) {
                cancel(Timeout, "verification timed out")
            }
        } finally {
            unsubscribeHandleOlmDecryptedVerificationRequestEvents()
        }
    }

    private suspend fun handleVerificationStepEvents(event: ClientEvent<VerificationStep>) {
        if (event is ToDeviceEvent) handleVerificationStepEvent(event.content, event.sender)
    }

    private suspend fun handleOlmDecryptedVerificationRequestEvents(event: DecryptedOlmEventContainer) {
        val content = event.decrypted.content
        if (content is VerificationStep) handleVerificationStepEvent(content, event.decrypted.sender)
    }

    private suspend fun handleVerificationStepEvent(step: VerificationStep, sender: UserId) {
        val eventTransactionId = step.transactionId
        if (eventTransactionId != null && eventTransactionId == transactionId
            && isVerificationRequestActive(timestamp, clock, state.value)
        ) {
            if (step is VerificationReadyEventContent) {
                val cancelDeviceIds = theirDeviceIds - step.fromDevice
                if (cancelDeviceIds.isNotEmpty()) {
                    val cancelEvent =
                        VerificationCancelEventContent(Accepted, "accepted by other device", relatesTo, transactionId)
                    try {
                        repository.sendToDevice(mapOf(theirUserId to cancelDeviceIds.associateWith {
                            olmEncryptionService.encryptOlm(cancelEvent, theirUserId, it).getOrNull() ?: cancelEvent
                        })).getOrThrow()
                    } catch (error: Exception) {
                        log.warn { "could not send cancel to other device ids ($cancelDeviceIds)" }
                    }
                }
            }
            handleIncomingVerificationStep(step, sender, false)
        }
    }
}

abstract class ActiveVerificationImpl(
    request: VerificationRequest,
    requestIsFromOurOwn: Boolean,
    protected val ownUserId: UserId,
    protected val ownDeviceId: String,
    override val theirUserId: UserId,
    theirInitialDeviceId: String?,
    override val timestamp: Long,
    protected val supportedMethods: Set<VerificationMethod>,
    final override val relatesTo: RelatesTo.Reference?,
    final override val transactionId: String?,
    protected val keyStore: OlmCryptoStore,
    private val keyTrustService: KeyTrustService,
    protected val json: Json,
) : ActiveVerification {
    final override var theirDeviceId: String? = theirInitialDeviceId
        private set

    private val mutex = Mutex()

    protected val mutableState: MutableStateFlow<ActiveVerificationState> =
        MutableStateFlow(
            if (requestIsFromOurOwn) ActiveVerificationState.OwnRequest(request)
            else ActiveVerificationState.TheirRequest(
                request,
                ownDeviceId,
                supportedMethods,
                relatesTo,
                transactionId,
                ::sendVerificationStepAndHandleIt
            )
        )
    override val state = mutableState.asStateFlow()

    private val lifecycleStarted = MutableStateFlow(false)
    protected abstract suspend fun lifecycle()
    internal suspend fun startLifecycle(scope: CoroutineScope): Boolean {
        log.debug { "start lifecycle of verification ${transactionId ?: relatesTo}" }
        return if (!lifecycleAlreadyStarted()) {
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                lifecycle()
                log.debug { "stop lifecycle of verification ${transactionId ?: relatesTo}" }
            }
            true
        } else false
    }

    private fun lifecycleAlreadyStarted() = lifecycleStarted.getAndUpdate { true }

    protected suspend fun handleIncomingVerificationStep(
        step: VerificationStep,
        sender: UserId,
        isOurOwn: Boolean
    ) {
        mutex.withLock { // we just want to be sure, that only one coroutine can access this simultaneously
            handleVerificationStep(step, sender, isOurOwn)
        }
    }

    private suspend fun handleVerificationStep(step: VerificationStep, sender: UserId, isOurOwn: Boolean) {
        try {
            log.debug { "handle verification step: $step from $sender" }
            if (sender != theirUserId && sender != ownUserId)
                cancel(Code.UserMismatch, "the user did not match the expected user, we want to verify")
            if (!(relatesTo != null && step.relatesTo == relatesTo || transactionId != null && step.transactionId == transactionId))
                cancel(Code.UnknownTransaction, "transaction is unknown")
            val currentState = state.value
            if (currentState is ActiveVerificationState.AcceptedByOtherDevice) {
                if (step is VerificationDoneEventContent) {
                    mutableState.value = ActiveVerificationState.Done
                }
                if (step is VerificationCancelEventContent) {
                    mutableState.value = ActiveVerificationState.Cancel(step, isOurOwn)
                }
            } else when (step) {
                is VerificationReadyEventContent -> {
                    if (currentState is ActiveVerificationState.OwnRequest || currentState is ActiveVerificationState.TheirRequest)
                        onReady(step)
                    else cancelUnexpectedMessage(currentState)
                }

                is VerificationStartEventContent -> {
                    if (currentState is ActiveVerificationState.Ready || currentState is ActiveVerificationState.Start)
                        onStart(step, sender, isOurOwn)
                    else cancelUnexpectedMessage(currentState)
                }

                is VerificationDoneEventContent -> {
                    if (currentState is ActiveVerificationState.Start || currentState is ActiveVerificationState.WaitForDone)
                        onDone(isOurOwn)
                    else cancelUnexpectedMessage(currentState)
                }

                is VerificationCancelEventContent -> {
                    onCancel(step, isOurOwn)
                }

                else -> when (currentState) {
                    is ActiveVerificationState.Start -> currentState.method.handleVerificationStep(step, isOurOwn)
                    else -> cancelUnexpectedMessage(currentState)
                }
            }
        } catch (error: Exception) {
            cancel(Code.InternalError, "something went wrong: ${error.message}")
        }
    }

    private suspend fun cancelUnexpectedMessage(currentState: ActiveVerificationState) {
        cancel(UnexpectedMessage, "this verification is at step ${currentState::class.simpleName}")
    }

    private fun onReady(step: VerificationReadyEventContent) {
        if (theirDeviceId == null && step.fromDevice != ownDeviceId) theirDeviceId = step.fromDevice
        mutableState.value = ActiveVerificationState.Ready(
            ownDeviceId,
            step.methods.intersect(supportedMethods),
            relatesTo,
            transactionId,
            ::sendVerificationStepAndHandleIt
        )
    }

    private suspend fun onStart(step: VerificationStartEventContent, sender: UserId, isOurOwn: Boolean) {
        val senderDevice = step.fromDevice
        val currentState = state.value
        suspend fun setNewStartEvent() {
            log.debug { "set new start event $step from $sender ($senderDevice)" }
            val method = when (step) {
                is SasStartEventContent ->
                    ActiveSasVerificationMethod.create(
                        startEventContent = step,
                        weStartedVerification = isOurOwn,
                        ownUserId = ownUserId,
                        ownDeviceId = ownDeviceId,
                        theirUserId = theirUserId,
                        theirDeviceId = theirDeviceId
                            ?: throw IllegalArgumentException("their device id should never be null at this step"),
                        relatesTo = relatesTo,
                        transactionId = transactionId,
                        sendVerificationStep = ::sendVerificationStepAndHandleIt,
                        keyStore = keyStore,
                        keyTrustService = keyTrustService,
                        json = json,
                    )
            }
            if (method != null) // the method already called cancel
                mutableState.value = ActiveVerificationState.Start(method, sender, senderDevice)
        }
        if (currentState is ActiveVerificationState.Start) {
            val currentStartContent = currentState.method.startEventContent
            if (currentStartContent is SasStartEventContent) {
                val userIdComparison = currentState.senderUserId.full.compareTo(sender.full)
                when {
                    userIdComparison > 0 -> setNewStartEvent()
                    userIdComparison < 0 -> {// do nothing (we keep the current Start)
                    }

                    else -> {
                        val deviceIdComparison = currentState.senderDeviceId.compareTo(step.fromDevice)
                        when {
                            deviceIdComparison > 0 -> setNewStartEvent()
                            else -> {// do nothing (we keep the current Start)
                            }
                        }
                    }
                }
            } else cancel(UnknownMethod, "the users selected two different verification methods")
        } else setNewStartEvent()
    }

    private fun onDone(isOurOwn: Boolean) {
        val oldState = mutableState.value
        val newState =
            if (oldState is ActiveVerificationState.WaitForDone && (isOurOwn && !oldState.isOurOwn || !isOurOwn && oldState.isOurOwn)) ActiveVerificationState.Done
            else ActiveVerificationState.WaitForDone(isOurOwn)
        mutableState.value = newState
    }

    private suspend fun onCancel(step: VerificationCancelEventContent, isOurOwn: Boolean) {
        mutableState.value = ActiveVerificationState.Cancel(step, isOurOwn)
        when (val currentState = state.value) {
            is ActiveVerificationState.Start -> {
                currentState.method.handleVerificationStep(step, isOurOwn)
            }

            else -> {}
        }
    }

    protected abstract suspend fun sendVerificationStep(step: VerificationStep)

    private suspend fun sendVerificationStepAndHandleIt(step: VerificationStep) {
        log.trace { "send verification step and handle it: $step" }
        when (step) {
            is VerificationCancelEventContent -> {
                if (state.value !is ActiveVerificationState.Cancel)
                    try {
                        sendVerificationStep(step)
                    } catch (error: Exception) {
                        log.warn(error) { "could not send cancel event: ${error.message}" }
                        // we just ignore when we could not send it, because it would time out on the other side anyway
                    }
                handleVerificationStep(step, ownUserId, true)
            }

            else -> try {
                sendVerificationStep(step)
                handleVerificationStep(step, ownUserId, true)
            } catch (error: Exception) {
                log.debug { "could not send step $step because: ${error.message}" }
                handleVerificationStep(
                    VerificationCancelEventContent(
                        Code.InternalError,
                        "problem sending step",
                        relatesTo,
                        transactionId
                    ), ownUserId,
                    true
                )
            }
        }
    }

    protected suspend fun cancel(code: Code, reason: String) {
        sendVerificationStepAndHandleIt(VerificationCancelEventContent(code, reason, relatesTo, transactionId))
    }

    override suspend fun cancel(message: String) {
        log.debug { "user cancelled verification" }
        cancel(Code.User, message)
    }
}
