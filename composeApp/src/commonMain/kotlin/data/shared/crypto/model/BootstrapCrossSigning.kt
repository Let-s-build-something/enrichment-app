package data.shared.crypto.model

import data.io.base.BaseResponse

data class BootstrapCrossSigning(
    val recoveryKey: String,
    val result: BaseResponse<Unit>
)