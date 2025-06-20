package data.io.matrix.user

import data.io.user.NetworkItemIO
import kotlinx.serialization.Serializable

@Serializable
data class SearchUserResponse(
    val limited: Boolean = true,
    val results: List<NetworkItemIO>? = null
)