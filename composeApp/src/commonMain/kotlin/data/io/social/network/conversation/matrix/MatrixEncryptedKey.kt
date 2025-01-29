package data.io.social.network.conversation.matrix

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatrixEncryptedKey(
    /** Required. Key operations. Must at least contain encrypt and decrypt. */
    @SerialName("key_ops")
    val keyOps: List<String>? = null,

    /** Required. The key, encoded as urlsafe unpadded base64. */
    val k: String? = null
)