package data.io.matrix.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
open class MatrixAuthenticationPlan(
    val session: String? = null,
    val flows: List<MatrixAuthenticationFlow>? = null,
    val params: MatrixAuthenticationParams? = null
)

@Serializable
data class MatrixAuthenticationParams(
    @SerialName("m.login.recaptcha")
    val recaptcha: MatrixAuthenticationRecaptcha? = null,
    @SerialName("m.login.terms")
    val terms: MatrixAuthenticationTerms? = null
)

@Serializable
data class MatrixAuthenticationRecaptcha(
    val publicKey: String? = null
)

@Serializable
data class MatrixAuthenticationTerms(
    val policies: Map<String, MatrixPolicyDocument>? = null
)

@Serializable
data class MatrixPolicyDocument(
    val version: String? = null,
    val en: MatrixPolicyPath? = null
)

@Serializable
data class MatrixPolicyPath(
    val name: String? = null,
    val url: String? = null
)

@Serializable
data class MatrixAuthenticationFlow(
    val type: String? = null,
    val stages: List<String>? = null,
    val identityProviders: List<MatrixIdentityProvider>? = null,

    @SerialName("org.matrix.msc3824.delegated_oidc_compatibility")
    val delegatedOidcCompatibility: Boolean = false
)

@Serializable
data class MatrixIdentityProvider(
    val id: String? = null,
    val name: String? = null,
    val icon: String? = null,
    val brand: String? = null
)