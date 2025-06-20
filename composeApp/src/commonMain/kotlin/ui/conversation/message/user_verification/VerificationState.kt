package ui.conversation.message.user_verification

import base.global.verification.ComparisonByUserData
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod

sealed class VerificationState {
    class TheirRequest(
        val methods: Set<VerificationMethod>
    ): VerificationState()

    data class ComparisonByUser(
        val data: ComparisonByUserData
    ): VerificationState()

    data object Success: VerificationState()
    data object Canceled: VerificationState()
    data object Hidden: VerificationState()
    data object Awaiting: VerificationState()
    data object Start: VerificationState()
    data object Ready: VerificationState()

    val isFinished: Boolean
        get() = this is Success || this is Canceled || this is Hidden
}