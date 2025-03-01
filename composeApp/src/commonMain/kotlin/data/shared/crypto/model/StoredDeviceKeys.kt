package data.shared.crypto.model

import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.keys.SignedDeviceKeys

@Serializable
data class StoredDeviceKeys(
    val value: SignedDeviceKeys,
    val trustLevel: KeySignatureTrustLevel
)
