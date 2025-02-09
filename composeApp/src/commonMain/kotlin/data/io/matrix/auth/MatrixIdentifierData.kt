package data.io.matrix.auth

import kotlinx.serialization.Serializable

/** Identification for the Matrix login */
@Serializable
data class MatrixIdentifierData(
    /** Matrix user_id or user localpart */
    val user: String? = null,

    /** email address */
    val address: String? = null,

    /** One of
    m.id.user
    m.id.thirdparty
    m.id.phone
     */
    val type: String?,

    /** the medium of login */
    val medium: String? = null
)