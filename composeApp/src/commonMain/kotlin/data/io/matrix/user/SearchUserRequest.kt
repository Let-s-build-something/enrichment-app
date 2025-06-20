package data.io.matrix.user

import kotlinx.serialization.Serializable

@Serializable
data class SearchUserRequest(
    val limit: Int,
    val searchTerm: String
)