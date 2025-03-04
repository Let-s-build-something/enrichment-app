package data.shared.crypto.verification

import data.shared.crypto.OlmCryptoStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.key.verification.SasAcceptEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.SasHash
import net.folivo.trixnity.core.model.events.m.key.verification.SasKeyAgreementProtocol
import net.folivo.trixnity.core.model.events.m.key.verification.SasMacEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.SasMessageAuthenticationCode
import net.folivo.trixnity.core.model.events.m.key.verification.SasMethod
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationStartEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationStep
import net.folivo.trixnity.core.model.keys.CrossSigningKeysUsage
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.core.model.keys.Keys
import net.folivo.trixnity.olm.OlmSAS

private val log = KotlinLogging.logger {}

sealed interface ActiveSasVerificationState {
    data class OwnSasStart(
        val content: VerificationStartEventContent.SasStartEventContent
    ) : ActiveSasVerificationState

    data class TheirSasStart(
        val content: VerificationStartEventContent.SasStartEventContent,
        private val olmSas: OlmSAS,
        private val json: Json,
        private val relatesTo: RelatesTo.Reference?,
        private val transactionId: String?,
        private val send: suspend (step: VerificationStep) -> Unit
    ) : ActiveSasVerificationState {
        suspend fun accept() {
            when {
                content.hashes.contains(SasHash.Sha256).not() -> {
                    send(
                        VerificationCancelEventContent(
                            VerificationCancelEventContent.Code.UnknownMethod,
                            "only hashes [${SasHash.Sha256.name}] are supported",
                            relatesTo,
                            transactionId
                        )
                    )
                }

                content.keyAgreementProtocols.contains(SasKeyAgreementProtocol.Curve25519HkdfSha256).not() -> {
                    send(
                        VerificationCancelEventContent(
                            VerificationCancelEventContent.Code.UnknownMethod,
                            "only key agreement protocols [${SasKeyAgreementProtocol.Curve25519HkdfSha256.name}] are supported",
                            relatesTo,
                            transactionId
                        )
                    )
                }

                content.messageAuthenticationCodes.contains(SasMessageAuthenticationCode.HkdfHmacSha256).not()
                        && content.messageAuthenticationCodes.contains(SasMessageAuthenticationCode.HkdfHmacSha256V2).not() -> {
                    send(
                        VerificationCancelEventContent(
                            VerificationCancelEventContent.Code.UnknownMethod,
                            "only message authentication codes [${SasMessageAuthenticationCode.HkdfHmacSha256.name} ${SasMessageAuthenticationCode.HkdfHmacSha256V2.name}] are supported",
                            relatesTo,
                            transactionId
                        )
                    )
                }

                content.shortAuthenticationString.contains(SasMethod.Decimal).not()
                        && content.shortAuthenticationString.contains(SasMethod.Emoji).not() -> {
                    send(
                        VerificationCancelEventContent(
                            VerificationCancelEventContent.Code.UnknownMethod,
                            "only short authentication strings [${SasMethod.Decimal.name} ${SasMethod.Emoji.name}] are supported",
                            relatesTo,
                            transactionId
                        )
                    )
                }

                else -> {
                    send(
                        SasAcceptEventContent(
                            commitment = createSasCommitment(
                                olmSas.publicKey,
                                content,
                                json
                            ),
                            hash = SasHash.Sha256,
                            keyAgreementProtocol = SasKeyAgreementProtocol.Curve25519HkdfSha256,
                            messageAuthenticationCode = content.messageAuthenticationCodes.let {
                                if (it.contains(SasMessageAuthenticationCode.HkdfHmacSha256V2)) SasMessageAuthenticationCode.HkdfHmacSha256V2
                                else SasMessageAuthenticationCode.HkdfHmacSha256
                            },
                            shortAuthenticationString = setOf(SasMethod.Decimal, SasMethod.Emoji),
                            relatesTo = relatesTo,
                            transactionId = transactionId
                        )
                    )
                }
            }
        }
    }

    data class Accept(val isOurOwn: Boolean) : ActiveSasVerificationState
    data class WaitForKeys(val isOurOwn: Boolean) : ActiveSasVerificationState
    data class ComparisonByUser(
        val decimal: List<Int>,
        val emojis: List<Pair<Int, String>>,
        private val ownUserId: UserId,
        private val ownDeviceId: String,
        private val theirUserId: UserId,
        private val theirDeviceId: String,
        private val messageAuthenticationCode: SasMessageAuthenticationCode,
        private val relatesTo: RelatesTo.Reference?,
        private val transactionId: String?,
        private val olmSas: OlmSAS,
        private val keyStore: OlmCryptoStore,
        private val send: suspend (stepContent: VerificationStep) -> Unit,
    ) : ActiveSasVerificationState {
        private val actualTransactionId = relatesTo?.eventId?.full
            ?: transactionId
            ?: throw IllegalArgumentException("actualTransactionId should never be null")

        suspend fun match() {
            when (messageAuthenticationCode) {
                SasMessageAuthenticationCode.HkdfHmacSha256 -> {
                    log.trace { "sendHkdfHmacSha256Mac with old (wrong) base64" }
                    sendHkdfHmacSha256Mac(olmSas::calculateMac)
                }

                SasMessageAuthenticationCode.HkdfHmacSha256V2 -> {
                    log.trace { "sendHkdfHmacSha256Mac with fixed base64" }
                    sendHkdfHmacSha256Mac(olmSas::calculateMacFixedBase64)
                }

                is SasMessageAuthenticationCode.Unknown -> {
                    send(
                        VerificationCancelEventContent(
                            VerificationCancelEventContent.Code.UnknownMethod,
                            "message authentication code not supported",
                            relatesTo,
                            transactionId
                        )
                    )
                }
            }
        }

        private suspend fun sendHkdfHmacSha256Mac(calculateMac: (String, String) -> String) {
            val baseInfo = "MATRIX_KEY_VERIFICATION_MAC" +
                    ownUserId.full + ownDeviceId +
                    theirUserId.full + theirDeviceId +
                    actualTransactionId
            val keysToMac = keyStore.getAllKeysFromUser<Key.Ed25519Key>(ownUserId, ownDeviceId,
                CrossSigningKeysUsage.MasterKey
            )
            if (keysToMac.isNotEmpty()) {
                val input = keysToMac.map { it.fullKeyId }.sortedBy { it }.joinToString(",")
                val info = baseInfo + "KEY_IDS"
                log.trace { "create keys mac from input $input and info $info" }
                val keys = calculateMac(input, info)
                val macs =
                    keysToMac.map {
                        log.trace { "create key mac from input $it and info ${baseInfo + it.fullKeyId}" }
                        it.copy(value = calculateMac(it.value, baseInfo + it.fullKeyId))
                    }
                send(SasMacEventContent(keys, Keys(macs.toSet()), relatesTo, transactionId))
            } else send(
                VerificationCancelEventContent(
                    VerificationCancelEventContent.Code.InternalError,
                    "no keys found",
                    relatesTo,
                    transactionId
                )
            )
        }


        suspend fun noMatch() {
            send(
                VerificationCancelEventContent(
                    VerificationCancelEventContent.Code.MismatchedSas,
                    "no match of SAS",
                    relatesTo,
                    transactionId
                )
            )
        }
    }

    data object WaitForMacs : ActiveSasVerificationState
}