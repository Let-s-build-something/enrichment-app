package data.io.matrix.room

import kotlinx.serialization.Serializable

@Serializable
data class MatrixEncryptedKey(
    /** Required. Key operations. Must at least contain encrypt and decrypt. */
    val keyOps: List<String>? = null,

    /** Required. The key, encoded as urlsafe unpadded base64. */
    val k: String? = null
)