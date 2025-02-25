package data.io.social.network.request

import data.io.base.paging.PaginationInfo
import data.io.base.paging.PaginationPageResponse
import data.io.user.NetworkItemIO
import kotlinx.serialization.Serializable

/** Response to the request for circling requests */
@Serializable
data class NetworkListResponse(
    override val pagination: PaginationInfo? = null,
    override val content: List<NetworkItemIO>
): PaginationPageResponse<NetworkItemIO>
