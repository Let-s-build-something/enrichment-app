package data.io.matrix.auth

import kotlinx.serialization.Serializable

/** Identification for the Matrix login */
@Serializable
data class MatrixIdentifierData(
    /** One of
    m.id.user
    m.id.thirdparty
    m.id.phone
     */
    val type: String,
    /** Matrix user_id or user localpart */
    val user: String
)