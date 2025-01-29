package data.io.social.network.conversation.matrix

import kotlinx.serialization.Serializable

@Serializable
data class MatrixEncryptedFile(
    /** Required. The URL to the file. */
    val url: String? = null,

    /** Required. A JSON Web Key object. */
    val key: MatrixEncryptedKey? = null,

    /** Required. The 128-bit unique counter block used by AES-CTR, encoded as unpadded base64. */
    val iv: String? = null,

    /** Required. A map from an algorithm name to a hash of the ciphertext, encoded as unpadded base64.
     *  Clients should support the SHA-256 hash, which uses the key sha256. */
    val hashes: Map<String, String>? = null
)