package data.io.base

import kotlinx.serialization.Serializable

/** Params from recaptcha result response */
@Serializable
data class RecaptchaParams(
    val token: String? = null
)