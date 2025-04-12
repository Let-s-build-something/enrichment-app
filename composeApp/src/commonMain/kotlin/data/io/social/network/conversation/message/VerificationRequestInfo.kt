package data.io.social.network.conversation.message

import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationRequest

@Serializable
data class VerificationRequestInfo(
    val to: String,
    override val fromDevice: String,
    override val methods: Set<VerificationMethod>
): VerificationRequest