package data.shared.crypto.verification

import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.RelatesTo

interface ActiveVerification {
    val theirUserId: UserId
    val timestamp: Long
    val relatesTo: RelatesTo.Reference?
    val transactionId: String?
    val state: StateFlow<ActiveVerificationState>

    val theirDeviceId: String?

    suspend fun cancel(message: String = "user cancelled verification")
}