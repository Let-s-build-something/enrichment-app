package data.io.social.network.request

import data.io.base.paging.PaginationInfo
import data.io.base.paging.PaginationPageResponse
import kotlinx.serialization.Serializable

/** Response to the request for circling requests */
@Serializable
data class CirclingRequestsResponse(
    override val pagination: PaginationInfo,
    override val content: List<CirclingRequest>
): PaginationPageResponse<CirclingRequest>