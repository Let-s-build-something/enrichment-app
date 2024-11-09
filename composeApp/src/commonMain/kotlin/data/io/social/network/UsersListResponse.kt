package data.io.social.network

import data.io.base.PaginationInfo
import data.io.base.PaginationPageResponse
import data.io.user.NetworkItemIO
import kotlinx.serialization.Serializable

/** Response to the search for users */
@Serializable
data class UsersListResponse(
    override val pagination: PaginationInfo,
    override val content: List<NetworkItemIO>
): PaginationPageResponse<NetworkItemIO>