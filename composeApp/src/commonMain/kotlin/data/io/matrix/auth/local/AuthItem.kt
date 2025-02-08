package data.io.matrix.auth.local

import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class AuthItem(
    @PrimaryKey
    val userIdHash: String?,

    val matrixHomeserver: String?,

    val loginType: String,

    /** the medium of login */
    val medium: String?
)
