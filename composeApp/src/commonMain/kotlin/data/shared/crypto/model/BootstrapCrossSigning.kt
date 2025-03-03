package data.shared.crypto.model

data class BootstrapCrossSigning(
    val recoveryKey: String,
    val result: Result<Unit>
)