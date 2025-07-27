package ui.login.sso

import kotlinx.serialization.Serializable

@Serializable
data class AugmySsoRequest(
    val type: String,
    val idToken: String,
    val issuer: String,
    val nonce: String,
    val deviceId: String,
    val displayName: String? = null
)